"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import axios from "axios";
import { createContext, useCallback, useContext, useEffect, useMemo } from "react";
import { limparConversasDoAssistente } from "@/components/conversaAssistente";
import { definirAoExpirarSessao } from "@/lib/api";
import { authService, type Credenciais } from "@/services/auth";
import type { Usuario } from "@/types";

interface AuthContexto {
  usuario: Usuario | null;
  carregando: boolean;
  /** A verificação da sessão falhou por motivo que não é "não logado" (servidor fora do ar, rede). */
  falhou: boolean;
  tentarNovamente: () => void;
  entrar: (credenciais: Credenciais) => Promise<void>;
  sair: () => Promise<void>;
}

const Contexto = createContext<AuthContexto | null>(null);

export const CHAVE_USUARIO = ["usuario"] as const;

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const client = useQueryClient();

  // A fonte da verdade é o servidor (GET /usuario/me), não um cache no navegador:
  // se o papel de alguém mudar, a próxima carga já reflete.
  const { data: usuario = null, isPending, isError, refetch } = useQuery({
    queryKey: CHAVE_USUARIO,
    queryFn: async () => {
      try {
        return await authService.eu();
      } catch (erro) {
        if (axios.isAxiosError(erro) && erro.response?.status === 401) return null; // sem sessão
        throw erro;
      }
    },
    staleTime: Infinity,
    // Só chega aqui erro que NÃO é 401 (o 401 vira "sem sessão" acima): uma falha momentânea do servidor
    // merece nova tentativa antes de incomodar o usuário.
    retry: 2,
    retryDelay: 800,
  });

  // Se nem a renovação do token salvar a sessão, esquece o usuário: a guarda de rota manda para o login.
  useEffect(() => {
    definirAoExpirarSessao(() => {
      client.setQueryData(CHAVE_USUARIO, null);
      client.removeQueries({ predicate: (q) => q.queryKey[0] !== CHAVE_USUARIO[0] });
    });
  }, [client]);

  const entrar = useCallback(
    async (credenciais: Credenciais) => {
      await authService.entrar(credenciais);
      client.setQueryData(CHAVE_USUARIO, await authService.eu());
    },
    [client],
  );

  const sair = useCallback(async () => {
    await authService.sair();
    client.setQueryData(CHAVE_USUARIO, null);
    client.removeQueries({ predicate: (q) => q.queryKey[0] !== CHAVE_USUARIO[0] }); // nada do usuário anterior sobra em cache
    limparConversasDoAssistente(); // nem a conversa com o assistente
  }, [client]);

  const valor = useMemo(
    () => ({ usuario, carregando: isPending, falhou: isError, tentarNovamente: () => void refetch(), entrar, sair }),
    [usuario, isPending, isError, refetch, entrar, sair],
  );

  return <Contexto.Provider value={valor}>{children}</Contexto.Provider>;
}

export function useAuth(): AuthContexto {
  const contexto = useContext(Contexto);
  if (!contexto) throw new Error("useAuth precisa estar dentro de <AuthProvider>");
  return contexto;
}
