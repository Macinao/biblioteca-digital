"use client";

import { formatarData } from "@/lib/format";
import type { Reserva } from "@/types";

interface Props {
  reservas: Reserva[];
  /** Mostra quem pegou o livro (visão do bibliotecário) em vez de só o livro. */
  mostrarUsuario?: boolean;
  /** Se informado, cada reserva ganha o botão "Registrar devolução". */
  aoDevolver?: (reserva: Reserva) => void;
  devolvendoId?: string;
}

export function EtiquetaStatus({ status }: { status: Reserva["statusReserva"] }) {
  const atrasado = status === "ATRASADO";
  return (
    <span
      className={`rounded px-2 py-0.5 text-xs font-semibold ${atrasado ? "bg-red-100 text-red-800" : "bg-marca-50 text-marca-700"}`}
    >
      {atrasado ? "Atrasado" : "Reservado"}
    </span>
  );
}

export default function ListaReservas({ reservas, mostrarUsuario = false, aoDevolver, devolvendoId }: Props) {
  if (reservas.length === 0) {
    return <p className="mt-4 text-gray-600">Nenhuma reserva ativa.</p>;
  }

  return (
    <ul className="mt-4 space-y-3">
      {reservas.map((reserva) => (
        <li
          key={reserva.id}
          className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-gray-200 bg-white p-4 shadow-sm"
        >
          <div>
            <p className="font-semibold text-marca-700">{reserva.tituloLivro}</p>
            <p className="text-sm text-gray-600">{reserva.autorLivro}</p>
            {mostrarUsuario && (
              <p className="text-sm text-gray-600">
                {reserva.nomeUsuario} ({reserva.matriculaUsuario})
              </p>
            )}
            <p className="mt-1 text-xs text-gray-500">
              Retirada em {formatarData(reserva.dataCriacao)} · devolver até {formatarData(reserva.dataDevolucao)}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <EtiquetaStatus status={reserva.statusReserva} />
            {aoDevolver && (
              <button
                type="button"
                onClick={() => aoDevolver(reserva)}
                disabled={devolvendoId === reserva.id}
                className="rounded border border-marca-700 px-3 py-1.5 text-sm font-semibold text-marca-700 hover:bg-marca-50 disabled:opacity-50"
              >
                {devolvendoId === reserva.id ? "Registrando…" : "Registrar devolução"}
              </button>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
