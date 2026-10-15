"use client";

import { useState } from "react";
import { mensagemDeErro } from "@/lib/api";
import { useReservar } from "@/hooks/useReservas";

/** Balcão da biblioteca: o bibliotecário registra o empréstimo de um exemplar para a matrícula do aluno. */
export default function FormReserva({ livroId, disponivel }: { livroId: string; disponivel: boolean }) {
  const [matricula, setMatricula] = useState("");
  const [aviso, setAviso] = useState<{ tipo: "ok" | "erro"; texto: string } | null>(null);
  const reservar = useReservar();

  function aoEnviar(e: React.FormEvent) {
    e.preventDefault();
    setAviso(null);
    reservar.mutate(
      { matricula: matricula.trim(), livroId },
      {
        onSuccess: (reserva) => {
          setMatricula("");
          setAviso({ tipo: "ok", texto: `Reserva registrada para ${reserva.nomeUsuario}.` });
        },
        onError: (erro) => setAviso({ tipo: "erro", texto: mensagemDeErro(erro, "Não foi possível reservar.") }),
      },
    );
  }

  return (
    <form onSubmit={aoEnviar} className="mt-4 rounded border border-gray-200 bg-white p-4">
      <h2 className="font-semibold text-marca-700">Reservar para um aluno</h2>
      <div className="mt-2 flex flex-wrap items-end gap-3">
        <div className="min-w-48 flex-1">
          <label htmlFor="matricula-reserva" className="mb-1 block text-sm font-medium text-gray-700">
            Matrícula do aluno
          </label>
          <input
            id="matricula-reserva"
            value={matricula}
            onChange={(e) => setMatricula(e.target.value)}
            required
            className="w-full rounded border border-gray-300 px-3 py-2"
          />
        </div>
        <button
          type="submit"
          disabled={!disponivel || reservar.isPending || matricula.trim() === ""}
          className="rounded bg-marca-700 px-4 py-2 font-semibold text-white hover:bg-marca-900 disabled:opacity-50"
        >
          {reservar.isPending ? "Reservando…" : "Reservar"}
        </button>
      </div>
      {!disponivel && <p className="mt-2 text-sm text-gray-600">Sem exemplares disponíveis no momento.</p>}
      {aviso && (
        <p
          role={aviso.tipo === "erro" ? "alert" : "status"}
          className={`mt-2 rounded p-2 text-sm ${aviso.tipo === "erro" ? "bg-red-50 text-red-800" : "bg-green-50 text-green-800"}`}
        >
          {aviso.texto}
        </p>
      )}
    </form>
  );
}
