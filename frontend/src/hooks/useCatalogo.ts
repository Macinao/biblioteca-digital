"use client";

import { keepPreviousData, useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { catalogosService, materiaisService, type TipoMaterial } from "@/services/materiais";

/** Itens por "carregar mais" na consulta e por página nas tabelas de gestão. */
export const TAMANHO_CONSULTA = 24;
export const TAMANHO_TABELA = 20;

// Mesmo teto do servidor (index.max_result_window do Elasticsearch): além dele o servidor responde 400.
const JANELA_MAXIMA = 10_000;

/** Atrasa o valor até o usuário parar de digitar: uma busca por pausa, não uma por tecla. */
export function useDebounce<T>(valor: T, ms = 350): T {
  const [atrasado, setAtrasado] = useState(valor);
  useEffect(() => {
    const id = setTimeout(() => setAtrasado(valor), ms);
    return () => clearTimeout(id);
  }, [valor, ms]);
  return atrasado;
}

/** Chave base de cada tipo: invalidar ["materiais", tipo] atualiza listas, buscas e detalhes de uma vez. */
export const chaveMateriais = (tipo: TipoMaterial) => ["materiais", tipo] as const;

/** Acervo em "carregar mais": busca só a próxima página quando pedido, com o filtro de curso feito no servidor. */
export function useMateriaisInfinitos(tipo: TipoMaterial, cursoId = "") {
  return useInfiniteQuery({
    queryKey: [...chaveMateriais(tipo), "lista", cursoId],
    queryFn: ({ pageParam }) => materiaisService.listarPagina(tipo, { pagina: pageParam, tamanho: TAMANHO_CONSULTA, cursoId }),
    initialPageParam: 0,
    getNextPageParam: (ultima, todas) => {
      const carregados = todas.reduce((soma, p) => soma + p.itens.length, 0);
      const proxima = todas.length;
      const cabeNaJanela = (proxima + 1) * TAMANHO_CONSULTA <= JANELA_MAXIMA;
      return carregados < ultima.total && cabeNaJanela ? proxima : undefined;
    },
  });
}

/** Uma página numerada (tabelas de gestão). Mantém a página anterior na tela enquanto a nova chega. */
export function useMateriaisPagina(tipo: TipoMaterial, pagina: number) {
  return useQuery({
    queryKey: [...chaveMateriais(tipo), "pagina", pagina],
    queryFn: () => materiaisService.listarPagina(tipo, { pagina, tamanho: TAMANHO_TABELA }),
    placeholderData: keepPreviousData,
  });
}

/** Busca textual (relevância, até 20 resultados). Só dispara com texto. */
export function useBuscaMateriais(tipo: TipoMaterial, texto: string) {
  const termo = texto.trim();
  return useQuery({
    queryKey: [...chaveMateriais(tipo), "busca", termo],
    queryFn: () => materiaisService.buscar(tipo, termo),
    enabled: termo !== "",
    placeholderData: keepPreviousData,
  });
}

/** Um item pelo id: o detalhe não baixa o acervo inteiro só para achar um livro. */
export function useMaterial(tipo: TipoMaterial, id: string) {
  return useQuery({
    queryKey: [...chaveMateriais(tipo), "detalhe", id],
    queryFn: () => materiaisService.obter(tipo, id),
  });
}

export function useCursos() {
  return useQuery({ queryKey: ["cursos"], queryFn: catalogosService.cursos, staleTime: 5 * 60_000 });
}

export function useTemas() {
  return useQuery({ queryKey: ["temas"], queryFn: catalogosService.temas, staleTime: 5 * 60_000 });
}
