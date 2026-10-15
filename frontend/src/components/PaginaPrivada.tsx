"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAuth } from "@/hooks/useAuth";
import { temPapel } from "@/lib/papeis";
import type { Papel } from "@/types";
import Cabecalho from "./Cabecalho";

interface Props {
  children: React.ReactNode;
  /** Papel mínimo para ver o conteúdo; sem isso, basta estar logado. */
  papelMinimo?: Papel;
}

/** Moldura das telas internas: exige login, mostra o cabeçalho e barra quem não tem o papel necessário. */
export default function PaginaPrivada({ children, papelMinimo = "ALUNO" }: Props) {
  const { usuario, carregando, falhou, tentarNovamente } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // Só manda para o login quando se SABE que não há sessão; falha do servidor não é "deslogado".
    if (!carregando && !usuario && !falhou) router.replace("/");
  }, [carregando, usuario, falhou, router]);

  if (!carregando && !usuario && falhou) {
    return (
      <div role="alert" className="grid min-h-screen place-items-center px-4">
        <div className="max-w-sm space-y-3 text-center">
          <p className="text-gray-800">Não foi possível verificar sua sessão agora. O servidor pode estar indisponível.</p>
          <button type="button" onClick={tentarNovamente} className="rounded bg-marca-700 px-4 py-2 font-semibold text-white hover:bg-marca-900">
            Tentar novamente
          </button>
        </div>
      </div>
    );
  }

  if (carregando || !usuario) {
    return (
      <div role="status" className="grid min-h-screen place-items-center text-marca-700">
        Carregando…
      </div>
    );
  }

  return (
    <div className="flex min-h-screen flex-col">
      <Cabecalho />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-6">
        {temPapel(usuario.role, papelMinimo) ? (
          children
        ) : (
          <div role="alert" className="rounded border border-red-200 bg-red-50 p-4 text-red-800">
            Você não tem permissão para acessar esta página.
          </div>
        )}
      </main>
    </div>
  );
}
