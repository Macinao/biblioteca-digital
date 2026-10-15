import { api } from "@/lib/api";
import type { Reserva } from "@/types";
import { lerTotal, type Pagina } from "@/lib/paginacao";

export const reservasService = {
  /** Reservas do usuário logado (a matrícula vem do token, não de parâmetro: ninguém vê as de outro). */
  async minhas(): Promise<Reserva[]> {
    const { data } = await api.get<Reserva[]>("/reserva/listar-minhas");
    return data;
  },

  /** Reservas de todos, uma página por vez (mais recentes primeiro) — só o bibliotecário. */
  async todasPagina(pagina: number, tamanho: number): Promise<Pagina<Reserva>> {
    const resposta = await api.get<Reserva[]>("/reserva/listar", { params: { pagina, tamanho } });
    return { itens: resposta.data, total: lerTotal(resposta.headers, resposta.data) };
  },

  /** O balcão registra o empréstimo informando a matrícula do aluno (só o bibliotecário). */
  async reservar(matricula: string, livroId: string): Promise<Reserva> {
    const { data } = await api.post<Reserva>("/reserva/reservar", { usuario: matricula, livro: livroId });
    return data;
  },

  async devolver(reservaId: string): Promise<void> {
    await api.put("/reserva/devolver", { reserva: reservaId });
  },
};
