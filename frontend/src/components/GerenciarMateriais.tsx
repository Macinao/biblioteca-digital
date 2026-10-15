"use client";

import { useState } from "react";
import { useBuscaMateriais, useDebounce, useMateriaisPagina, TAMANHO_TABELA } from "@/hooks/useCatalogo";
import { useRemoverMaterial, useSalvarMaterial } from "@/hooks/useGerenciar";
import { mensagemDeErro } from "@/lib/api";
import { formatarData } from "@/lib/format";
import { cursosDe, MATERIAIS, type TipoMaterial } from "@/services/materiais";
import type { Material, Papel } from "@/types";
import Dialogo from "./Dialogo";
import FormMaterial from "./FormMaterial";
import Paginador from "./Paginador";
import PaginaPrivada from "./PaginaPrivada";

type Modal = { modo: "novo" } | { modo: "editar"; material: Material } | { modo: "excluir"; material: Material } | null;

interface Props {
  tipo: TipoMaterial;
  /** Título da tela (ex.: "Meus materiais" para o professor). Padrão: o rótulo do tipo. */
  titulo?: string;
  papelMinimo?: Papel;
}

/** Cadastro, edição e exclusão de qualquer tipo de material, a partir da tabela de configuração. */
export default function GerenciarMateriais({ tipo, titulo, papelMinimo }: Props) {
  return (
    <PaginaPrivada papelMinimo={papelMinimo ?? MATERIAIS[tipo].papelEscrita}>
      <Conteudo tipo={tipo} titulo={titulo} />
    </PaginaPrivada>
  );
}

function Conteudo({ tipo, titulo }: { tipo: TipoMaterial; titulo?: string }) {
  const config = MATERIAIS[tipo];
  const [pagina, setPagina] = useState(0);
  const [filtro, setFiltro] = useState("");
  const [modal, setModal] = useState<Modal>(null);
  const [erroOperacao, setErroOperacao] = useState("");
  const termo = useDebounce(filtro).trim();

  // Sem texto: tabela paginada pelo servidor. Com texto: os resultados mais relevantes da busca (até 20).
  const paginada = useMateriaisPagina(tipo, pagina);
  const busca = useBuscaMateriais(tipo, termo);
  const ativa = termo ? busca : paginada;
  const { isPending, isError, error } = ativa;
  const materiais = termo ? busca.data : paginada.data?.itens;
  const visiveis = materiais ?? [];
  const salvar = useSalvarMaterial(tipo);
  const remover = useRemoverMaterial(tipo);

  function fechar() {
    setModal(null);
    setErroOperacao("");
  }

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-marca-700">{titulo ?? `Gerenciar ${config.rotulo.toLowerCase()}`}</h1>
        <button
          type="button"
          onClick={() => setModal({ modo: "novo" })}
          className="rounded bg-marca-700 px-4 py-2 font-semibold text-white hover:bg-marca-900"
        >
          Novo {config.rotuloSingular.toLowerCase()}
        </button>
      </div>

      <div className="mt-4">
        <label htmlFor="filtro" className="sr-only">Buscar por título, autor ou assunto</label>
        <input
          id="filtro"
          type="search"
          value={filtro}
          onChange={(e) => setFiltro(e.target.value)}
          placeholder="Buscar por título, autor ou assunto"
          className="w-full max-w-md rounded border border-gray-300 bg-white px-3 py-2"
        />
      </div>

      {isPending && <p role="status" className="mt-4">Carregando…</p>}
      {isError && <p role="alert" className="mt-4 rounded bg-red-50 p-3 text-red-800">{mensagemDeErro(error, "Não foi possível carregar a lista.")}</p>}

      {materiais && (
        <div className="mt-4 overflow-x-auto rounded-lg border border-gray-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-marca-50 text-marca-700">
              <tr>
                <th className="px-3 py-2">Título</th>
                <th className="px-3 py-2">Autor</th>
                <th className="px-3 py-2">{config.cursoUnico ? "Curso" : "Cursos"}</th>
                <th className="px-3 py-2">Publicação</th>
                {config.temQuantidade && <th className="px-3 py-2">Exemplares</th>}
                <th className="px-3 py-2 text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {visiveis.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-3 py-6 text-center text-gray-500">Nenhum item encontrado.</td>
                </tr>
              )}
              {visiveis.map((m) => (
                <tr key={m.id} className="border-t border-gray-100">
                  <td className="px-3 py-2 font-medium">{m.titulo}</td>
                  <td className="px-3 py-2">{m.autor}</td>
                  <td className="px-3 py-2">{cursosDe(m).map((c) => c.nome).join(", ")}</td>
                  <td className="px-3 py-2">{formatarData(m.dataPublicacao)}</td>
                  {config.temQuantidade && <td className="px-3 py-2">{m.quantidade}</td>}
                  <td className="space-x-2 px-3 py-2 text-right whitespace-nowrap">
                    <button type="button" onClick={() => setModal({ modo: "editar", material: m })} aria-label={`Editar ${m.titulo}`} className="rounded border border-marca-700 px-2 py-1 text-marca-700 hover:bg-marca-50">
                      Editar
                    </button>
                    <button type="button" onClick={() => setModal({ modo: "excluir", material: m })} aria-label={`Excluir ${m.titulo}`} className="rounded border border-red-700 px-2 py-1 text-red-700 hover:bg-red-50">
                      Excluir
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!termo && paginada.data && (
        <Paginador pagina={pagina} total={paginada.data.total} tamanho={TAMANHO_TABELA} aoMudar={setPagina} ocupado={paginada.isPlaceholderData} />
      )}
      {termo && <p className="mt-3 text-sm text-gray-600">Resultados mais relevantes da busca (até 20). Limpe o campo para ver o acervo paginado.</p>}

      {(modal?.modo === "novo" || modal?.modo === "editar") && (
        <Dialogo titulo={modal.modo === "novo" ? `Novo ${config.rotuloSingular.toLowerCase()}` : `Editar ${config.rotuloSingular.toLowerCase()}`} aoFechar={fechar}>
          <FormMaterial
            tipo={tipo}
            material={modal.modo === "editar" ? modal.material : undefined}
            salvando={salvar.isPending}
            erro={erroOperacao}
            aoCancelar={fechar}
            aoSalvar={(dados, arquivos) => {
              setErroOperacao("");
              salvar.mutate(
                { id: modal.modo === "editar" ? modal.material.id : undefined, dados, arquivos },
                { onSuccess: fechar, onError: (e) => setErroOperacao(mensagemDeErro(e, "Não foi possível salvar.")) },
              );
            }}
          />
        </Dialogo>
      )}

      {modal?.modo === "excluir" && (
        <Dialogo titulo="Confirmar exclusão" aoFechar={fechar}>
          <p>
            Excluir <strong>{modal.material.titulo}</strong>? Essa ação remove o item do acervo.
          </p>
          {erroOperacao && <p role="alert" className="mt-3 rounded bg-red-50 p-2 text-sm text-red-800">{erroOperacao}</p>}
          <div className="mt-5 flex justify-end gap-3">
            <button type="button" onClick={fechar} className="rounded border border-gray-300 px-4 py-2 hover:bg-gray-50">Cancelar</button>
            <button
              type="button"
              disabled={remover.isPending}
              onClick={() =>
                remover.mutate(modal.material.id, {
                  onSuccess: fechar,
                  onError: (e) => setErroOperacao(mensagemDeErro(e, "Não foi possível excluir.")),
                })
              }
              className="rounded bg-red-700 px-4 py-2 font-semibold text-white hover:bg-red-800 disabled:opacity-60"
            >
              {remover.isPending ? "Excluindo…" : "Excluir"}
            </button>
          </div>
        </Dialogo>
      )}
    </>
  );
}
