import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { Material } from "@/types";
import ListagemMateriais from "../ListagemMateriais";

const listarPagina = jest.fn();
const buscar = jest.fn();
const cursos = jest.fn();

jest.mock("next/navigation", () => ({ useRouter: () => ({ replace: jest.fn() }), usePathname: () => "/livros" }));
jest.mock("@/hooks/useAuth", () => ({
  useAuth: () => ({ usuario: { nome: "Ana", role: "ALUNO" }, carregando: false, sair: jest.fn() }),
}));
jest.mock("@/services/materiais", () => ({
  ...jest.requireActual("@/services/materiais"),
  materiaisService: {
    listarPagina: (...a: unknown[]) => listarPagina(...a),
    buscar: (...a: unknown[]) => buscar(...a),
    urlCapa: () => "http://api/capa.png",
  },
  catalogosService: { cursos: () => cursos(), temas: jest.fn() },
}));

const material = (n: number, curso = { id: "c1", nome: "Computação" }, quantidade = 3): Material => ({
  id: String(n),
  titulo: `Livro ${n}`,
  autor: "Autor",
  descricao: "d",
  temas: [],
  dataPublicacao: "2012-01-01T00:00:00",
  cursos: [curso],
  quantidade,
});

function renderizar() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <ListagemMateriais tipo="livro" />
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  listarPagina.mockResolvedValue({ itens: [material(1), material(2, { id: "c2", nome: "Direito" }, 0)], total: 2 });
  cursos.mockResolvedValue([
    { id: "c1", nome: "Computação" },
    { id: "c2", nome: "Direito" },
  ]);
});

describe("ListagemMateriais", () => {
  it("lista os livros com disponibilidade e o total", async () => {
    renderizar();

    expect(await screen.findByText("Livro 1")).toBeInTheDocument();
    expect(screen.getByText("3 disponível(is)")).toBeInTheDocument();
    expect(screen.getByText("Indisponível")).toBeInTheDocument();
    expect(screen.getByText("Mostrando 2 de 2")).toBeInTheDocument();
    expect(listarPagina).toHaveBeenCalledWith("livro", { pagina: 0, tamanho: 24, cursoId: "" });
  });

  it("pede só a primeira página e carrega a próxima quando solicitado", async () => {
    const primeira = Array.from({ length: 24 }, (_, i) => material(i));
    const segunda = Array.from({ length: 26 }, (_, i) => material(100 + i));
    listarPagina
      .mockResolvedValueOnce({ itens: primeira, total: 50 })
      .mockResolvedValueOnce({ itens: segunda.slice(0, 24), total: 50 })
      .mockResolvedValueOnce({ itens: segunda.slice(24), total: 50 });
    renderizar();
    expect(await screen.findByText("Mostrando 24 de 50")).toBeInTheDocument();
    expect(listarPagina).toHaveBeenCalledTimes(1); // não baixou o acervo inteiro

    await userEvent.click(screen.getByRole("button", { name: "Carregar mais" }));

    expect(await screen.findByText("Mostrando 48 de 50")).toBeInTheDocument();
    expect(listarPagina).toHaveBeenLastCalledWith("livro", { pagina: 1, tamanho: 24, cursoId: "" });

    await userEvent.click(screen.getByRole("button", { name: "Carregar mais" }));
    expect(await screen.findByText("Mostrando 50 de 50")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Carregar mais" })).not.toBeInTheDocument();
  });

  it("filtrar por curso consulta o servidor (o filtro não pode valer só para a página carregada)", async () => {
    renderizar();
    await screen.findByText("Livro 1");
    listarPagina.mockResolvedValue({ itens: [material(2, { id: "c2", nome: "Direito" }, 0)], total: 1 });

    await userEvent.selectOptions(screen.getByLabelText("Filtrar por curso"), "c2");

    await waitFor(() => expect(listarPagina).toHaveBeenLastCalledWith("livro", { pagina: 0, tamanho: 24, cursoId: "c2" }));
    expect(await screen.findByText("Mostrando 1 de 1")).toBeInTheDocument();
    expect(screen.queryByText("Livro 1")).not.toBeInTheDocument();
  });

  it("digitar chama a busca uma vez depois que a pessoa para de digitar, e o curso filtra os resultados", async () => {
    buscar.mockResolvedValue([material(1), material(2, { id: "c2", nome: "Direito" })]);
    renderizar();
    await screen.findByText("Livro 1");

    await userEvent.type(screen.getByLabelText(/buscar em livros/i), "algoritmo");

    await waitFor(() => expect(buscar).toHaveBeenCalledWith("livro", "algoritmo"));
    expect(buscar).toHaveBeenCalledTimes(1); // não houve uma busca por tecla
    expect(await screen.findByText("2 resultado(s)")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Carregar mais" })).not.toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Filtrar por curso"), "c2");
    await waitFor(() => expect(screen.queryByText("Livro 1")).not.toBeInTheDocument());
    expect(screen.getByText("1 resultado(s)")).toBeInTheDocument();
  });

  it("avisa quando nada é encontrado", async () => {
    listarPagina.mockResolvedValue({ itens: [], total: 0 });
    renderizar();

    expect(await screen.findByText(/nada encontrado/i)).toBeInTheDocument();
  });

  it("mostra o erro da API em vez de uma tela vazia", async () => {
    listarPagina.mockRejectedValue(new Error("falhou"));
    renderizar();

    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível carregar o acervo.");
  });
});
