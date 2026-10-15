"use client";

import { useMemo, useState } from "react";
import { mensagemDeErro } from "@/lib/api";
import { useBuscaMateriais, useCursos, useDebounce, useMateriaisInfinitos } from "@/hooks/useCatalogo";
import { cursosDe, MATERIAIS, type TipoMaterial } from "@/services/materiais";
import CardMaterial from "./CardMaterial";
import PaginaPrivada from "./PaginaPrivada";

/** Tela de consulta única para livros, livros públicos, TCCs e materiais de estudo. */
export default function ListagemMateriais({ tipo }: { tipo: TipoMaterial }) {
  // O conteúdo fica num componente interno: seus hooks só rodam depois que a PaginaPrivada confirmou o login.
  return (
    <PaginaPrivada>
      <Conteudo tipo={tipo} />
    </PaginaPrivada>
  );
}

function Conteudo({ tipo }: { tipo: TipoMaterial }) {
  const config = MATERIAIS[tipo];
  const [texto, setTexto] = useState("");
  const [cursoId, setCursoId] = useState("");
  const busca = useDebounce(texto).trim();
  const buscando = busca !== "";

  const { data: cursos = [] } = useCursos();

  // Dois modos: sem texto, navega pelo acervo paginado (filtro de curso no servidor); com texto, mostra os
  // resultados por relevância (poucos, no máximo 20), e aí o filtro de curso é aplicado sobre eles.
  const navegacao = useMateriaisInfinitos(tipo, cursoId);
  const resultadoBusca = useBuscaMateriais(tipo, busca);
  const ativa = buscando ? resultadoBusca : navegacao;

  const itens = useMemo(() => {
    if (!buscando) return navegacao.data?.pages.flatMap((p) => p.itens) ?? [];
    const achados = resultadoBusca.data ?? [];
    return cursoId ? achados.filter((m) => cursosDe(m).some((c) => c.id === cursoId)) : achados;
  }, [buscando, navegacao.data, resultadoBusca.data, cursoId]);

  const total = buscando ? itens.length : (navegacao.data?.pages[0]?.total ?? 0);
  const carregando = ativa.isPending;

  return (
    <>
      <h1 className="text-2xl font-bold text-marca-700">{config.rotulo}</h1>

      <form role="search" onSubmit={(e) => e.preventDefault()} className="mt-4 flex flex-wrap gap-3">
        <div className="min-w-60 flex-1">
          <label htmlFor="busca" className="sr-only">
            Buscar em {config.rotulo.toLowerCase()}
          </label>
          <input
            id="busca"
            type="search"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Buscar por título, autor ou assunto"
            className="w-full rounded border border-gray-300 bg-white px-3 py-2"
          />
        </div>
        <div>
          <label htmlFor="filtro-curso" className="sr-only">
            Filtrar por curso
          </label>
          <select
            id="filtro-curso"
            value={cursoId}
            onChange={(e) => setCursoId(e.target.value)}
            className="rounded border border-gray-300 bg-white px-3 py-2"
          >
            <option value="">Todos os cursos</option>
            {cursos.map((curso) => (
              <option key={curso.id} value={curso.id}>
                {curso.nome}
              </option>
            ))}
          </select>
        </div>
      </form>

      <div aria-live="polite" className="mt-4 text-sm text-gray-600">
        {ativa.isFetching && !carregando
          ? "Buscando…"
          : !carregando && !ativa.isError
            ? buscando
              ? `${total} resultado(s)`
              : `Mostrando ${itens.length} de ${total}`
            : ""}
      </div>

      {carregando && <p role="status">Carregando…</p>}
      {ativa.isError && (
        <p role="alert" className="rounded bg-red-50 p-3 text-red-800">
          {mensagemDeErro(ativa.error, "Não foi possível carregar o acervo.")}
        </p>
      )}
      {!carregando && !ativa.isError && itens.length === 0 && (
        <p className="mt-4 text-gray-600">Nada encontrado. Tente outro termo ou outro curso.</p>
      )}

      <ul className="mt-2 grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {itens.map((material) => (
          <CardMaterial key={material.id} tipo={tipo} material={material} />
        ))}
      </ul>

      {!buscando && navegacao.hasNextPage && (
        <div className="mt-6 text-center">
          <button
            type="button"
            onClick={() => navegacao.fetchNextPage()}
            disabled={navegacao.isFetchingNextPage}
            className="rounded border border-marca-700 px-5 py-2 font-semibold text-marca-700 hover:bg-marca-50 disabled:opacity-60"
          >
            {navegacao.isFetchingNextPage ? "Carregando…" : "Carregar mais"}
          </button>
        </div>
      )}
    </>
  );
}
