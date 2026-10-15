import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

// withCredentials: os tokens JWT vivem em cookies HttpOnly (o JavaScript nunca os lê); o navegador só os reenvia.
export const api = axios.create({ baseURL: API_URL, withCredentials: true });

type RequisicaoRetentavel = InternalAxiosRequestConfig & { _jaRenovou?: boolean };

const ROTAS_SEM_RENOVACAO = ["/auth/login", "/auth/refresh", "/auth/logout"];

let renovacaoEmAndamento: Promise<void> | null = null;
let aoExpirarSessao: () => void = () => {};

/** O AuthProvider registra aqui o que fazer quando nem a renovação salva a sessão (voltar ao login). */
export function definirAoExpirarSessao(callback: () => void) {
  aoExpirarSessao = callback;
}

// Várias requisições podem tomar 401 ao mesmo tempo (access token venceu): todas esperam UMA renovação.
function renovarSessao(): Promise<void> {
  renovacaoEmAndamento ??= api
    .post("/auth/refresh")
    .then(() => undefined)
    .finally(() => {
      renovacaoEmAndamento = null;
    });
  return renovacaoEmAndamento;
}

api.interceptors.response.use(
  (resposta) => resposta,
  async (erro: AxiosError) => {
    const original = erro.config as RequisicaoRetentavel | undefined;
    const ehRotaDeAuth = ROTAS_SEM_RENOVACAO.some((rota) => original?.url?.includes(rota));

    if (erro.response?.status === 401 && original && !original._jaRenovou && !ehRotaDeAuth) {
      original._jaRenovou = true; // uma única nova tentativa por requisição: sem laço infinito
      try {
        await renovarSessao();
      } catch {
        aoExpirarSessao();
        return Promise.reject(erro);
      }
      return api(original);
    }
    return Promise.reject(erro);
  },
);

/** Texto pronto para mostrar ao usuário a partir de qualquer erro de chamada à API. */
export function mensagemDeErro(erro: unknown, padrao = "Não foi possível concluir a operação."): string {
  if (!axios.isAxiosError(erro)) return padrao;
  if (!erro.response) return "Sem conexão com o servidor. Tente novamente em instantes.";
  if (erro.response.status === 429) return "Muitas tentativas. Aguarde um minuto e tente de novo.";
  const corpo = erro.response.data as { message?: string; details?: string } | undefined;
  return corpo?.details || corpo?.message || padrao;
}
