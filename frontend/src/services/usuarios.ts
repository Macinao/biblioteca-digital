import { api } from "@/lib/api";
import type { NovoUsuario } from "@/types";

export const usuariosService = {
  /** Só o bibliotecário cadastra usuários (o cadastro público não existe). */
  async cadastrar(usuario: NovoUsuario): Promise<void> {
    await api.post("/usuario", usuario);
  },
};
