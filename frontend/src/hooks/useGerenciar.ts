"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { catalogosService, materiaisService, type ArquivosMaterial, type DadosMaterial, type TipoCatalogo, type TipoMaterial } from "@/services/materiais";
import { usuariosService } from "@/services/usuarios";
import { chaveMateriais } from "./useCatalogo";
import { invalidarComAtraso } from "./useReservas";

export function useSalvarMaterial(tipo: TipoMaterial) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, dados, arquivos }: { id?: string; dados: DadosMaterial; arquivos: ArquivosMaterial }) =>
      id ? materiaisService.atualizar(tipo, id, dados, arquivos) : materiaisService.criar(tipo, dados, arquivos),
    onSuccess: () => invalidarComAtraso(client, [chaveMateriais(tipo)]),
  });
}

export function useRemoverMaterial(tipo: TipoMaterial) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => materiaisService.remover(tipo, id),
    onSuccess: () => invalidarComAtraso(client, [chaveMateriais(tipo)]),
  });
}

/** Renomear/excluir curso ou tema altera os materiais que o citam (o search-service refaz esses documentos). */
function useInvalidacaoDeCatalogo(tipo: TipoCatalogo) {
  const client = useQueryClient();
  return () => invalidarComAtraso(client, [[tipo === "curso" ? "cursos" : "temas"], ["materiais"]]);
}

export function useCriarCatalogo(tipo: TipoCatalogo) {
  const invalidar = useInvalidacaoDeCatalogo(tipo);
  return useMutation({ mutationFn: (nome: string) => catalogosService.criar(tipo, nome), onSuccess: invalidar });
}

export function useRenomearCatalogo(tipo: TipoCatalogo) {
  const invalidar = useInvalidacaoDeCatalogo(tipo);
  return useMutation({
    mutationFn: ({ id, nome }: { id: string; nome: string }) => catalogosService.renomear(tipo, id, nome),
    onSuccess: invalidar,
  });
}

export function useRemoverCatalogo(tipo: TipoCatalogo) {
  const invalidar = useInvalidacaoDeCatalogo(tipo);
  return useMutation({ mutationFn: (id: string) => catalogosService.remover(tipo, id), onSuccess: invalidar });
}

export function useCadastrarUsuario() {
  return useMutation({ mutationFn: usuariosService.cadastrar });
}
