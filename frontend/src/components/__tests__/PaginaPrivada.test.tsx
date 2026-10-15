import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { Papel } from "@/types";
import PaginaPrivada from "../PaginaPrivada";

const replace = jest.fn();
const sair = jest.fn();
const tentarNovamente = jest.fn();
let sessao: { usuario: { nome: string; role: Papel } | null; carregando: boolean; falhou?: boolean };

jest.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  usePathname: () => "/livros",
}));
jest.mock("@/hooks/useAuth", () => ({ useAuth: () => ({ falhou: false, ...sessao, sair, tentarNovamente }) }));

beforeEach(() => {
  jest.clearAllMocks();
});

describe("PaginaPrivada", () => {
  it("sem sessão manda para o login e não mostra o conteúdo", () => {
    sessao = { usuario: null, carregando: false };

    render(<PaginaPrivada>conteudo secreto</PaginaPrivada>);

    expect(replace).toHaveBeenCalledWith("/");
    expect(screen.queryByText("conteudo secreto")).not.toBeInTheDocument();
  });

  it("falha do servidor ao verificar a sessão NÃO manda para o login: oferece tentar de novo", async () => {
    sessao = { usuario: null, carregando: false, falhou: true };

    render(<PaginaPrivada>conteudo</PaginaPrivada>);

    expect(replace).not.toHaveBeenCalled();
    expect(screen.getByRole("alert")).toHaveTextContent("Não foi possível verificar sua sessão");
    await userEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(tentarNovamente).toHaveBeenCalledTimes(1);
  });

  it("enquanto carrega a sessão não redireciona", () => {
    sessao = { usuario: null, carregando: true };

    render(<PaginaPrivada>conteudo</PaginaPrivada>);

    expect(replace).not.toHaveBeenCalled();
    expect(screen.getByRole("status")).toHaveTextContent("Carregando");
  });

  it("aluno vê o conteúdo e só os itens de menu do seu papel", () => {
    sessao = { usuario: { nome: "Ana", role: "ALUNO" }, carregando: false };

    render(<PaginaPrivada>conteudo</PaginaPrivada>);

    expect(screen.getByText("conteudo")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Livros" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Meus materiais" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Gerenciar" })).not.toBeInTheDocument();
  });

  it("bibliotecário vê todos os itens, inclusive Gerenciar", () => {
    sessao = { usuario: { nome: "Bia", role: "BIBLIOTECARIO" }, carregando: false };

    render(<PaginaPrivada>conteudo</PaginaPrivada>);

    expect(screen.getByRole("link", { name: "Gerenciar" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Meus materiais" })).toBeInTheDocument();
  });

  it("papel insuficiente para a página mostra aviso em vez do conteúdo", () => {
    sessao = { usuario: { nome: "Ana", role: "ALUNO" }, carregando: false };

    render(<PaginaPrivada papelMinimo="BIBLIOTECARIO">area restrita</PaginaPrivada>);

    expect(screen.getByRole("alert")).toHaveTextContent("não tem permissão");
    expect(screen.queryByText("area restrita")).not.toBeInTheDocument();
  });
});
