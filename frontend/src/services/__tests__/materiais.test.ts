import { api } from "@/lib/api";
import { cursosDe, materiaisService, MATERIAIS, TIPOS_MATERIAL, catalogosService } from "../materiais";

const post = jest.spyOn(api, "post");
const put = jest.spyOn(api, "put");

beforeEach(() => {
  post.mockReset().mockResolvedValue({ data: {} });
  put.mockReset().mockResolvedValue({ data: {} });
});

const dados = {
  titulo: "T",
  autor: "A",
  descricao: "D",
  dataPublicacao: "2020-01-01T00:00:00",
  temas: ["t1"],
  cursos: ["c1"],
};

// jsdom não tem Blob.text(): lê pelo FileReader.
function lerTexto(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const leitor = new FileReader();
    leitor.onload = () => resolve(String(leitor.result));
    leitor.onerror = reject;
    leitor.readAsText(blob);
  });
}

async function jsonDaParte(form: FormData, parte: string) {
  const blob = form.get(parte) as Blob;
  return { tipo: blob.type, corpo: JSON.parse(await lerTexto(blob)) };
}

describe("contrato multipart com o core-service", () => {
  it.each([
    ["livro", "/livro/cadastrar", "livro"],
    ["livro-publico", "/livro-publico/cadastrar", "livro-publico"],
    ["tcc", "/tcc/cadastrar", "tcc"],
    ["material-estudo", "/material-estudo/cadastrar", "material-estudo"],
  ] as const)("%s envia a parte JSON '%s' com Content-Type application/json", async (tipo, url, parte) => {
    await materiaisService.criar(tipo, dados, {});

    const [urlChamada, form] = post.mock.calls[0] as [string, FormData];
    expect(urlChamada).toBe(url);
    const json = await jsonDaParte(form, parte);
    expect(json.tipo).toBe("application/json"); // sem isso o Spring não converte a parte em objeto
    expect(json.corpo).toMatchObject({ titulo: "T", temas: ["t1"] });
  });

  it("anexa capa e conteúdo com os nomes de parte exigidos", async () => {
    const capa = new File(["x"], "capa.png", { type: "image/png" });
    const pdf = new File(["x"], "a.pdf", { type: "application/pdf" });

    await materiaisService.criar("livro-publico", dados, { capa, conteudo: pdf });

    const form = post.mock.calls[0][1] as FormData;
    expect((form.get("capa") as File).name).toBe("capa.png");
    expect((form.get("conteudo") as File).name).toBe("a.pdf");
  });

  it("atualizar inclui o id no JSON e não anexa arquivo que não foi trocado", async () => {
    await materiaisService.atualizar("tcc", "abc", dados, { capa: null, conteudo: null });

    const [url, form] = put.mock.calls[0] as [string, FormData];
    expect(url).toBe("/tcc/atualizar");
    expect((await jsonDaParte(form, "tcc")).corpo.id).toBe("abc");
    expect(form.has("conteudo")).toBe(false);
  });

  it("curso e tema usam os nomes de campo do backend na atualização", async () => {
    await catalogosService.renomear("curso", "1", "Novo");
    await catalogosService.renomear("tema", "2", "Outro");

    expect(put).toHaveBeenNthCalledWith(1, "/curso/atualizar", { cursoId: "1", novoCurso: "Novo" });
    expect(put).toHaveBeenNthCalledWith(2, "/tema/atualizar", { temaId: "2", novoTema: "Outro" });
  });
});

describe("configuração dos tipos", () => {
  it("só o material de estudo é escrito por professor; o resto por bibliotecário", () => {
    expect(TIPOS_MATERIAL.filter((t) => MATERIAIS[t].papelEscrita === "PROFESSOR")).toEqual(["material-estudo"]);
  });

  it("TCC tem um curso só e vira lista para a tela", () => {
    const curso = { id: "c", nome: "Direito" };
    expect(cursosDe({ curso } as never)).toEqual([curso]);
    expect(cursosDe({ cursos: [curso, curso] } as never)).toHaveLength(2);
    expect(cursosDe({} as never)).toEqual([]);
  });
});
