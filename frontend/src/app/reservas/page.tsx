"use client";

import ListaReservas from "@/components/ListaReservas";
import PaginaPrivada from "@/components/PaginaPrivada";
import { useMinhasReservas } from "@/hooks/useReservas";
import { LIMITE_RESERVAS_POR_USUARIO } from "@/types";
import { mensagemDeErro } from "@/lib/api";

export default function MinhasReservas() {
  return (
    <PaginaPrivada>
      <Conteudo />
    </PaginaPrivada>
  );
}

function Conteudo() {
  const { data: reservas, isPending, isError, error } = useMinhasReservas();

  return (
    <>
      <h1 className="text-2xl font-bold text-marca-700">Minhas reservas</h1>
      {reservas && (
        <p className="mt-1 text-sm text-gray-600">
          {reservas.length} de {LIMITE_RESERVAS_POR_USUARIO} reservas ativas. A devolução é registrada no balcão da biblioteca.
        </p>
      )}
      {isPending && <p role="status">Carregando…</p>}
      {isError && (
        <p role="alert" className="mt-3 rounded bg-red-50 p-3 text-red-800">
          {mensagemDeErro(error, "Não foi possível carregar suas reservas.")}
        </p>
      )}
      {reservas && <ListaReservas reservas={reservas} />}
    </>
  );
}
