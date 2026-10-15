import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AxiosError, type AxiosResponse } from "axios";
import Login from "../page";

const replace = jest.fn();
const entrar = jest.fn();
let sessao: { usuario: unknown; carregando: boolean } = { usuario: null, carregando: false };

jest.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));
jest.mock("@/hooks/useAuth", () => ({ useAuth: () => ({ ...sessao, entrar }) }));

beforeEach(() => {
  jest.clearAllMocks();
  sessao = { usuario: null, carregando: false };
});

describe("tela de login", () => {
  it("exige matrícula e senha antes de chamar a API", async () => {
    render(<Login />);

    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByText("Informe sua matrícula.")).toBeInTheDocument();
    expect(screen.getByText("Informe sua senha.")).toBeInTheDocument();
    expect(entrar).not.toHaveBeenCalled();
  });

  it("entra com as credenciais digitadas e vai para o início", async () => {
    entrar.mockResolvedValue(undefined);
    render(<Login />);

    await userEvent.type(screen.getByLabelText("Matrícula"), "ADMIN001");
    await userEvent.type(screen.getByLabelText("Senha"), "Segredo123");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/home"));
    expect(entrar).toHaveBeenCalledWith({ matricula: "ADMIN001", senha: "Segredo123" });
  });

  it("mostra a mensagem da API quando as credenciais estão erradas e não navega", async () => {
    entrar.mockRejectedValue(
      new AxiosError("x", "401", undefined, null, {
        status: 401,
        data: { message: "Credenciais inválidas", details: "Matrícula ou senha incorretas." },
      } as AxiosResponse),
    );
    render(<Login />);

    await userEvent.type(screen.getByLabelText("Matrícula"), "ADMIN001");
    await userEvent.type(screen.getByLabelText("Senha"), "errada");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Matrícula ou senha incorretas.");
    expect(replace).not.toHaveBeenCalledWith("/home");
  });

  it("quem já tem sessão é levado direto ao início", () => {
    sessao = { usuario: { nome: "Maria" }, carregando: false };

    render(<Login />);

    expect(replace).toHaveBeenCalledWith("/home");
  });

  it("não há link de cadastro público: só o bibliotecário cria contas", () => {
    render(<Login />);

    expect(screen.queryByRole("link", { name: /cadastr/i })).not.toBeInTheDocument();
    expect(screen.getByText(/procure o bibliotecário/i)).toBeInTheDocument();
  });
});
