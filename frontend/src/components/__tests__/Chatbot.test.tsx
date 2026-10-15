import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AxiosError, type AxiosResponse } from "axios";
import Chatbot from "../Chatbot";
import { limparConversasDoAssistente } from "../conversaAssistente";

const enviar = jest.fn();
jest.mock("@/services/chatbot", () => ({ chatbotService: { enviar: (m: string) => enviar(m) } }));

beforeEach(() => {
  jest.clearAllMocks();
  sessionStorage.clear();
});

async function abrir() {
  render(<Chatbot matricula="2023001" />);
  await userEvent.click(screen.getByRole("button", { name: "Assistente" }));
}

describe("Chatbot", () => {
  it("abre fechado e mostra a boas-vindas ao abrir", async () => {
    render(<Chatbot matricula="2023001" />);
    expect(screen.queryByRole("log")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Assistente" }));

    expect(screen.getByRole("log")).toHaveTextContent("Posso ajudar a encontrar livros");
  });

  it("envia a pergunta, mostra a resposta e limpa o campo", async () => {
    enviar.mockResolvedValue("Encontrei **Código Limpo**.");
    await abrir();

    await userEvent.type(screen.getByLabelText("Mensagem"), "algum livro de código?");
    await userEvent.click(screen.getByRole("button", { name: "Enviar" }));

    expect(await screen.findByText("Código Limpo")).toBeInTheDocument();
    expect(enviar).toHaveBeenCalledWith("algum livro de código?");
    expect(screen.getByLabelText("Mensagem")).toHaveValue("");
    expect(screen.getByText("Código Limpo").tagName).toBe("STRONG");
  });

  it("transforma lista em itens e NUNCA interpreta HTML vindo do modelo", async () => {
    enviar.mockResolvedValue("- primeiro\n- <img src=x onerror=alert(1)>");
    await abrir();

    await userEvent.type(screen.getByLabelText("Mensagem"), "oi");
    await userEvent.click(screen.getByRole("button", { name: "Enviar" }));

    expect(await screen.findByText("primeiro")).toBeInTheDocument();
    expect(screen.getByText("<img src=x onerror=alert(1)>")).toBeInTheDocument(); // texto literal
    expect(document.querySelector("img")).toBeNull();
  });

  it("mostra mensagem amigável quando o serviço falha (ex.: limite de mensagens)", async () => {
    enviar.mockRejectedValue(new AxiosError("x", "429", undefined, null, { status: 429, data: {} } as AxiosResponse));
    await abrir();

    await userEvent.type(screen.getByLabelText("Mensagem"), "oi");
    await userEvent.click(screen.getByRole("button", { name: "Enviar" }));

    expect(await screen.findByText(/muitas tentativas/i)).toBeInTheDocument();
  });

  it("a conversa sobrevive a desmontar e montar de novo (trocar de página / recarregar)", async () => {
    enviar.mockResolvedValue("Resposta guardada");
    const primeira = render(<Chatbot matricula="2023001" />);
    await userEvent.click(screen.getByRole("button", { name: "Assistente" }));
    await userEvent.type(screen.getByLabelText("Mensagem"), "minha pergunta");
    await userEvent.click(screen.getByRole("button", { name: "Enviar" }));
    await screen.findByText("Resposta guardada");

    primeira.unmount();
    render(<Chatbot matricula="2023001" />);
    await userEvent.click(screen.getByRole("button", { name: "Assistente" }));

    expect(screen.getByText("minha pergunta")).toBeInTheDocument();
    expect(screen.getByText("Resposta guardada")).toBeInTheDocument();
  });

  it("depois de recarregar, novas mensagens continuam a numeração sem repetir ids", async () => {
    enviar.mockResolvedValue("segunda resposta");
    sessionStorage.setItem(
      "smartteca-chat:2023001",
      JSON.stringify([{ id: 0, autor: "bot", texto: "oi" }, { id: 1, autor: "usuario", texto: "antes" }, { id: 2, autor: "bot", texto: "resposta antiga" }]),
    );
    await abrir();

    await userEvent.type(screen.getByLabelText("Mensagem"), "depois");
    await userEvent.click(screen.getByRole("button", { name: "Enviar" }));

    await screen.findByText("segunda resposta");
    const ids = JSON.parse(sessionStorage.getItem("smartteca-chat:2023001")!).map((m: { id: number }) => m.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it("cada usuário tem a sua conversa; outra matrícula começa do zero", async () => {
    sessionStorage.setItem("smartteca-chat:2023001", JSON.stringify([{ id: 0, autor: "usuario", texto: "segredo da Ana" }]));

    render(<Chatbot matricula="2023002" />);
    await userEvent.click(screen.getByRole("button", { name: "Assistente" }));

    expect(screen.queryByText("segredo da Ana")).not.toBeInTheDocument();
    expect(screen.getByRole("log")).toHaveTextContent("Posso ajudar a encontrar livros");
  });

  it("conteúdo corrompido na sessão não quebra: começa uma conversa nova", async () => {
    sessionStorage.setItem("smartteca-chat:2023001", "{isso não é json");

    await abrir();

    expect(screen.getByRole("log")).toHaveTextContent("Posso ajudar a encontrar livros");
  });

  it("o logout apaga as conversas guardadas de todos", () => {
    sessionStorage.setItem("smartteca-chat:2023001", "[]");
    sessionStorage.setItem("smartteca-chat:2023002", "[]");
    sessionStorage.setItem("outra-coisa", "mantida");

    limparConversasDoAssistente();

    expect(sessionStorage.getItem("smartteca-chat:2023001")).toBeNull();
    expect(sessionStorage.getItem("smartteca-chat:2023002")).toBeNull();
    expect(sessionStorage.getItem("outra-coisa")).toBe("mantida");
  });

  it("não envia mensagem vazia", async () => {
    await abrir();

    expect(screen.getByRole("button", { name: "Enviar" })).toBeDisabled();
  });
});
