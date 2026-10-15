import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from "axios";
import { AxiosError } from "axios";
import { api, definirAoExpirarSessao, mensagemDeErro } from "../api";

function resposta(config: InternalAxiosRequestConfig, status: number, data: unknown = {}): AxiosResponse {
  return { data, status, statusText: "", headers: {}, config };
}

/** Adaptador falso: cada URL tem uma fila de status a devolver; registra as chamadas. */
function instalarServidor(roteiro: Record<string, number[]>) {
  const chamadas: string[] = [];
  const adapter: AxiosAdapter = async (config) => {
    const url = config.url ?? "";
    chamadas.push(url);
    const status = roteiro[url]?.shift() ?? 200;
    if (status >= 400) {
      throw new AxiosError("falha", String(status), config, null, resposta(config, status));
    }
    return resposta(config, status);
  };
  api.defaults.adapter = adapter;
  return chamadas;
}

afterEach(() => definirAoExpirarSessao(() => {}));

describe("renovação de sessão", () => {
  it("renova uma vez e repete a requisição que tomou 401", async () => {
    const chamadas = instalarServidor({ "/livro/listar": [401, 200] });

    const r = await api.get("/livro/listar");

    expect(r.status).toBe(200);
    expect(chamadas).toEqual(["/livro/listar", "/auth/refresh", "/livro/listar"]);
  });

  it("várias requisições com 401 ao mesmo tempo disparam UMA só renovação", async () => {
    const chamadas = instalarServidor({ "/a": [401], "/b": [401], "/c": [401] });

    await Promise.all([api.get("/a"), api.get("/b"), api.get("/c")]);

    expect(chamadas.filter((u) => u === "/auth/refresh")).toHaveLength(1);
  });

  it("se a renovação falha, avisa que a sessão expirou e devolve o erro original", async () => {
    const aoExpirar = jest.fn();
    definirAoExpirarSessao(aoExpirar);
    instalarServidor({ "/livro/listar": [401], "/auth/refresh": [401] });

    await expect(api.get("/livro/listar")).rejects.toMatchObject({ response: { status: 401 } });

    expect(aoExpirar).toHaveBeenCalledTimes(1);
  });

  it("não entra em laço: segundo 401 depois de renovar vira erro", async () => {
    const chamadas = instalarServidor({ "/livro/listar": [401, 401] });

    await expect(api.get("/livro/listar")).rejects.toBeDefined();

    expect(chamadas.filter((u) => u === "/auth/refresh")).toHaveLength(1);
  });

  it("login com 401 (senha errada) não tenta renovar", async () => {
    const chamadas = instalarServidor({ "/auth/login": [401] });

    await expect(api.post("/auth/login")).rejects.toBeDefined();

    expect(chamadas).toEqual(["/auth/login"]);
  });
});

describe("mensagemDeErro", () => {
  const erroHttp = (status: number, data?: unknown) =>
    new AxiosError("x", String(status), undefined, null, { status, data } as AxiosResponse);

  it("prefere o detalhe da API, depois a mensagem, depois o padrão", () => {
    expect(mensagemDeErro(erroHttp(422, { message: "Regra", details: "Sem exemplares" }))).toBe("Sem exemplares");
    expect(mensagemDeErro(erroHttp(422, { message: "Regra" }))).toBe("Regra");
    expect(mensagemDeErro(erroHttp(500, {}), "Falhou")).toBe("Falhou");
  });

  it("trata falta de conexão e rate limit com texto próprio", () => {
    expect(mensagemDeErro(new AxiosError("Network Error"))).toMatch(/sem conexão/i);
    expect(mensagemDeErro(erroHttp(429, {}))).toMatch(/muitas tentativas/i);
  });

  it("erro que não é de HTTP vira o texto padrão", () => {
    expect(mensagemDeErro(new Error("boom"), "Padrão")).toBe("Padrão");
  });
});
