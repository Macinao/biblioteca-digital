import { api } from "@/lib/api";
import type { Usuario } from "@/types";

export interface Credenciais {
  matricula: string;
  senha: string;
}

export const authService = {
  /** O backend responde com os cookies HttpOnly; o corpo traz tokens que o front não usa. */
  async entrar(credenciais: Credenciais): Promise<void> {
    await api.post("/auth/login", credenciais);
  },

  async sair(): Promise<void> {
    try {
      await api.post("/auth/logout");
    } catch {
      // sem sessão ou sem rede: o usuário quer sair do mesmo jeito
    }
  },

  async eu(): Promise<Usuario> {
    const { data } = await api.get<Usuario>("/usuario/me");
    return data;
  },
};
