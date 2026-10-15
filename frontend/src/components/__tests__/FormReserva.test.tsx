import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AxiosError, type AxiosResponse } from "axios";
import FormReserva from "../FormReserva";

const reservar = jest.fn();
jest.mock("@/services/reservas", () => ({ reservasService: { reservar: (...a: unknown[]) => reservar(...a) } }));

function renderizar(disponivel = true) {
  const client = new QueryClient({ defaultOptions: { mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <FormReserva livroId="livro-1" disponivel={disponivel} />
    </QueryClientProvider>,
  );
}

beforeEach(() => jest.clearAllMocks());

describe("FormReserva", () => {
  it("reserva para a matrícula informada e confirma com o nome do aluno", async () => {
    reservar.mockResolvedValue({ nomeUsuario: "Ana Aluna" });
    renderizar();

    await userEvent.type(screen.getByLabelText("Matrícula do aluno"), "ALUNO0001");
    await userEvent.click(screen.getByRole("button", { name: "Reservar" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Reserva registrada para Ana Aluna.");
    expect(reservar).toHaveBeenCalledWith("ALUNO0001", "livro-1");
    expect(screen.getByLabelText("Matrícula do aluno")).toHaveValue("");
  });

  it("mostra a regra de negócio que a API recusou (ex.: limite de 3 reservas)", async () => {
    reservar.mockRejectedValue(
      new AxiosError("x", "422", undefined, null, {
        status: 422,
        data: { details: "Usuário atingiu o limite de 3 reservas ativas." },
      } as AxiosResponse),
    );
    renderizar();

    await userEvent.type(screen.getByLabelText("Matrícula do aluno"), "ALUNO0001");
    await userEvent.click(screen.getByRole("button", { name: "Reservar" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("limite de 3 reservas ativas");
  });

  it("não deixa reservar sem matrícula nem sem exemplar disponível", async () => {
    renderizar(false);

    expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();
    await userEvent.type(screen.getByLabelText("Matrícula do aluno"), "ALUNO0001");
    expect(screen.getByRole("button", { name: "Reservar" })).toBeDisabled();
    expect(screen.getByText(/sem exemplares disponíveis/i)).toBeInTheDocument();
  });
});
