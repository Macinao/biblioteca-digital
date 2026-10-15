import { render, screen } from "@testing-library/react";
import AssistenteGlobal from "../AssistenteGlobal";

let sessao: { usuario: { matricula: string } | null };
jest.mock("@/hooks/useAuth", () => ({ useAuth: () => sessao }));
jest.mock("@/services/chatbot", () => ({ chatbotService: { enviar: jest.fn() } }));

describe("AssistenteGlobal", () => {
  it("não aparece para quem não está logado (tela de login)", () => {
    sessao = { usuario: null };

    render(<AssistenteGlobal />);

    expect(screen.queryByRole("button", { name: "Assistente" })).not.toBeInTheDocument();
  });

  it("aparece uma vez só para quem está logado", () => {
    sessao = { usuario: { matricula: "2023001" } };

    render(<AssistenteGlobal />);

    expect(screen.getAllByRole("button", { name: "Assistente" })).toHaveLength(1);
  });
});
