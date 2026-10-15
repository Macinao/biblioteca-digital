"use client";

import { useState } from "react";
import { useCursos, useTemas } from "@/hooks/useCatalogo";
import { useCriarCatalogo, useRemoverCatalogo, useRenomearCatalogo } from "@/hooks/useGerenciar";
import { mensagemDeErro } from "@/lib/api";
import { CATALOGOS, type TipoCatalogo } from "@/services/materiais";
import PaginaPrivada from "./PaginaPrivada";

/** Cursos e temas: lista com criar, renomear e excluir. */
export default function GerenciarCatalogo({ tipo }: { tipo: TipoCatalogo }) {
  return (
    <PaginaPrivada papelMinimo="BIBLIOTECARIO">
      <Conteudo tipo={tipo} />
    </PaginaPrivada>
  );
}

function Conteudo({ tipo }: { tipo: TipoCatalogo }) {
  const config = CATALOGOS[tipo];
  const cursos = useCursos();
  const temas = useTemas();
  const { data: itens, isPending, isError, error } = tipo === "curso" ? cursos : temas;

  const criar = useCriarCatalogo(tipo);
  const renomear = useRenomearCatalogo(tipo);
  const remover = useRemoverCatalogo(tipo);

  const [novo, setNovo] = useState("");
  const [editando, setEditando] = useState<{ id: string; nome: string } | null>(null);
  const [erroOperacao, setErroOperacao] = useState("");

  const falhou = (e: unknown) => setErroOperacao(mensagemDeErro(e));

  return (
    <>
      <h1 className="text-2xl font-bold text-marca-700">{config.rotulo}</h1>

      <form
        onSubmit={(e) => {
          e.preventDefault();
          setErroOperacao("");
          criar.mutate(novo.trim(), { onSuccess: () => setNovo(""), onError: falhou });
        }}
        className="mt-4 flex flex-wrap items-end gap-3"
      >
        <div className="min-w-60 flex-1">
          <label htmlFor="novo-nome" className="mb-1 block text-sm font-medium text-gray-700">Novo {config.rotuloSingular.toLowerCase()}</label>
          <input id="novo-nome" value={novo} onChange={(e) => setNovo(e.target.value)} required className="w-full rounded border border-gray-300 px-3 py-2" />
        </div>
        <button type="submit" disabled={criar.isPending || novo.trim() === ""} className="rounded bg-marca-700 px-4 py-2 font-semibold text-white hover:bg-marca-900 disabled:opacity-50">
          Adicionar
        </button>
      </form>

      {erroOperacao && <p role="alert" className="mt-3 rounded bg-red-50 p-2 text-sm text-red-800">{erroOperacao}</p>}
      {isPending && <p role="status" className="mt-4">Carregando…</p>}
      {isError && <p role="alert" className="mt-4 rounded bg-red-50 p-3 text-red-800">{mensagemDeErro(error)}</p>}

      <ul className="mt-4 divide-y divide-gray-100 rounded-lg border border-gray-200 bg-white">
        {(itens ?? []).map((item) => (
          <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-2">
            {editando?.id === item.id ? (
              <form
                className="flex flex-1 flex-wrap items-center gap-2"
                onSubmit={(e) => {
                  e.preventDefault();
                  setErroOperacao("");
                  renomear.mutate({ id: item.id, nome: editando.nome.trim() }, { onSuccess: () => setEditando(null), onError: falhou });
                }}
              >
                <label htmlFor={`edit-${item.id}`} className="sr-only">Novo nome de {item.nome}</label>
                <input id={`edit-${item.id}`} value={editando.nome} onChange={(e) => setEditando({ id: item.id, nome: e.target.value })} required className="min-w-48 flex-1 rounded border border-gray-300 px-2 py-1" />
                <button type="submit" disabled={renomear.isPending} className="rounded bg-marca-700 px-3 py-1 text-white">Salvar</button>
                <button type="button" onClick={() => setEditando(null)} className="rounded border border-gray-300 px-3 py-1">Cancelar</button>
              </form>
            ) : (
              <>
                <span>{item.nome}</span>
                <span className="space-x-2">
                  <button type="button" onClick={() => setEditando({ id: item.id, nome: item.nome })} aria-label={`Renomear ${item.nome}`} className="rounded border border-marca-700 px-2 py-1 text-sm text-marca-700 hover:bg-marca-50">
                    Renomear
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      if (window.confirm(`Excluir "${item.nome}"?`)) {
                        setErroOperacao("");
                        remover.mutate(item.id, { onError: falhou });
                      }
                    }}
                    aria-label={`Excluir ${item.nome}`}
                    className="rounded border border-red-700 px-2 py-1 text-sm text-red-700 hover:bg-red-50"
                  >
                    Excluir
                  </button>
                </span>
              </>
            )}
          </li>
        ))}
      </ul>
    </>
  );
}
