import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { TipoMaterial } from "@/services/materiais";
import FormMaterial from "../FormMaterial";

jest.mock("@/services/materiais", () => ({
  ...jest.requireActual("@/services/materiais"),
  catalogosService: {
    cursos: async () => [
      { id: "c1", nome: "Computação" },
      { id: "c2", nome: "Direito" },
    ],
    temas: async () => [{ id: "t1", nome: "Algoritmos" }],
  },
}));

const aoSalvar = jest.fn();

function renderizar(tipo: TipoMaterial, material?: Parameters<typeof FormMaterial>[0]["material"]) {
  const client = new QueryClient();
  return render(
    <QueryClientProvider client={client}>
      <FormMaterial tipo={tipo} material={material} salvando={false} aoSalvar={aoSalvar} aoCancelar={jest.fn()} />
    </QueryClientProvider>,
  );
}

async function preencherBasico() {
  await userEvent.type(await screen.findByLabelText("Título"), "Clean Code"); // o formulário só monta com cursos e temas carregados
  await userEvent.type(screen.getByLabelText("Autor"), "Martin");
  await userEvent.type(screen.getByLabelText("Descrição"), "Boas práticas");
  await userEvent.type(screen.getByLabelText("Data de publicação"), "2008-08-01");
}

beforeEach(() => jest.clearAllMocks());

describe("FormMaterial", () => {
  it("não envia nada e aponta cada campo obrigatório vazio", async () => {
    renderizar("livro");
    await screen.findByLabelText("Computação");

    await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));

    expect(await screen.findByText("Informe o título.")).toBeInTheDocument();
    expect(screen.getByText("Escolha ao menos um curso.")).toBeInTheDocument();
    expect(screen.getByText("Escolha ao menos um tema.")).toBeInTheDocument();
    expect(screen.getByText("Envie a capa.")).toBeInTheDocument();
    expect(aoSalvar).not.toHaveBeenCalled();
  });

  it("livro: envia ids de cursos e temas, data no formato da API, quantidade numérica e a capa", async () => {
    renderizar("livro");
    await preencherBasico();
    await userEvent.click(await screen.findByLabelText("Computação"));
    await userEvent.click(screen.getByLabelText("Algoritmos"));
    await userEvent.clear(screen.getByLabelText("Exemplares"));
    await userEvent.type(screen.getByLabelText("Exemplares"), "4");
    const capa = new File(["x"], "capa.png", { type: "image/png" });
    await userEvent.upload(screen.getByLabelText(/Capa/), capa);

    await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));

    await waitFor(() => expect(aoSalvar).toHaveBeenCalledTimes(1));
    const [dados, arquivos] = aoSalvar.mock.calls[0];
    expect(dados).toEqual({
      titulo: "Clean Code",
      autor: "Martin",
      descricao: "Boas práticas",
      dataPublicacao: "2008-08-01T00:00:00",
      temas: ["t1"],
      cursos: ["c1"],
      quantidade: 4,
    });
    expect(arquivos.capa).toBe(capa);
  });

  it("TCC: escolhe um curso só (campo 'curso', não 'cursos') e exige o PDF", async () => {
    renderizar("tcc");
    await preencherBasico();
    await userEvent.selectOptions(await screen.findByLabelText("Curso"), "c2");
    await userEvent.click(screen.getByLabelText("Algoritmos"));

    await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));
    expect(await screen.findByText("Envie o PDF.")).toBeInTheDocument();

    await userEvent.upload(screen.getByLabelText(/Arquivo PDF/), new File(["%PDF"], "tcc.pdf", { type: "application/pdf" }));
    await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));

    await waitFor(() => expect(aoSalvar).toHaveBeenCalled());
    const dados = aoSalvar.mock.calls[0][0];
    expect(dados.curso).toBe("c2");
    expect(dados).not.toHaveProperty("cursos");
    expect(dados).not.toHaveProperty("quantidade");
  });

  it("rejeita arquivo com formato errado", async () => {
    renderizar("tcc");
    await screen.findByLabelText("Curso");

    await userEvent.upload(screen.getByLabelText(/Arquivo PDF/), new File(["x"], "foto.png", { type: "image/png" }), {
      applyAccept: false,
    });
    await userEvent.click(screen.getByRole("button", { name: "Cadastrar" }));

    expect(await screen.findByText(/formato inválido/i)).toBeInTheDocument();
  });

  it("edição: vem preenchida e os arquivos viram opcionais", async () => {
    renderizar("tcc", {
      id: "x",
      titulo: "Meu TCC",
      autor: "Eu",
      descricao: "d",
      dataPublicacao: "2024-05-10T00:00:00",
      temas: [{ id: "t1", nome: "Algoritmos" }],
      curso: { id: "c1", nome: "Computação" },
    });
    await screen.findByLabelText("Curso");

    expect(screen.getByLabelText("Título")).toHaveValue("Meu TCC");
    expect(screen.getByLabelText("Data de publicação")).toHaveValue("2024-05-10");
    await userEvent.click(screen.getByRole("button", { name: "Salvar alterações" }));

    await waitFor(() => expect(aoSalvar).toHaveBeenCalled());
    expect(aoSalvar.mock.calls[0][1]).toEqual({ capa: null, conteudo: null });
  });
});
