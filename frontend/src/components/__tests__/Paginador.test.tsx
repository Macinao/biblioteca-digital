import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import Paginador from "../Paginador";

const aoMudar = jest.fn();

beforeEach(() => jest.clearAllMocks());

describe("Paginador", () => {
  it("na primeira página só avança", async () => {
    render(<Paginador pagina={0} total={45} tamanho={20} aoMudar={aoMudar} />);

    expect(screen.getByText("Página 1 de 3 · 45 itens")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Próxima" }));
    expect(aoMudar).toHaveBeenCalledWith(1);
  });

  it("na última página só volta", async () => {
    render(<Paginador pagina={2} total={45} tamanho={20} aoMudar={aoMudar} />);

    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
    await userEvent.click(screen.getByRole("button", { name: "Anterior" }));
    expect(aoMudar).toHaveBeenCalledWith(1);
  });

  it("com um item só usa o singular e não habilita nada", () => {
    render(<Paginador pagina={0} total={1} tamanho={20} aoMudar={aoMudar} />);

    expect(screen.getByText("Página 1 de 1 · 1 item")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
  });

  it("congela os botões enquanto carrega a página nova", () => {
    render(<Paginador pagina={1} total={100} tamanho={20} aoMudar={aoMudar} ocupado />);

    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
  });
});
