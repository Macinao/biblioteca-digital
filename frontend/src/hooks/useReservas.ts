"use client";

import { keepPreviousData, useMutation, useQuery, useQueryClient, type QueryClient, type QueryKey } from "@tanstack/react-query";
import { reservasService } from "@/services/reservas";
import { chaveMateriais } from "./useCatalogo";

export const CHAVE_MINHAS_RESERVAS = ["reservas", "minhas"] as const;
export const TAMANHO_PAGINA_RESERVAS = 20;

export function useMinhasReservas() {
  return useQuery({ queryKey: CHAVE_MINHAS_RESERVAS, queryFn: reservasService.minhas });
}

/** Reservas de todos (bibliotecário), uma página por vez. */
export function useTodasReservas(pagina: number) {
  return useQuery({
    queryKey: ["reservas", "todas", pagina],
    queryFn: () => reservasService.todasPagina(pagina, TAMANHO_PAGINA_RESERVAS),
    placeholderData: keepPreviousData,
  });
}

/**
 * Escritas vão para o core-service (Postgres) e as leituras vêm do search-service (Elasticsearch), que se
 * atualiza por eventos do Kafka — um instante depois. Por isso invalidamos já e de novo logo em seguida,
 * para a tela não mostrar o estoque/lista antigos.
 */
export function invalidarComAtraso(client: QueryClient, chaves: QueryKey[], atrasoMs = 1500) {
  const invalidar = () => chaves.forEach((queryKey) => client.invalidateQueries({ queryKey }));
  invalidar();
  setTimeout(invalidar, atrasoMs);
}

function useInvalidacaoDeReservas() {
  const client = useQueryClient();
  return () => invalidarComAtraso(client, [["reservas"], chaveMateriais("livro")]);
}

export function useReservar() {
  const invalidar = useInvalidacaoDeReservas();
  return useMutation({
    mutationFn: ({ matricula, livroId }: { matricula: string; livroId: string }) =>
      reservasService.reservar(matricula, livroId),
    onSuccess: invalidar,
  });
}

export function useDevolver() {
  const invalidar = useInvalidacaoDeReservas();
  return useMutation({ mutationFn: reservasService.devolver, onSuccess: invalidar });
}
