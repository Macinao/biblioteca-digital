"use client";

import Link from "next/link";
import axios from "axios";
import { useMaterial } from "@/hooks/useCatalogo";
import { useAuth } from "@/hooks/useAuth";
import { mensagemDeErro } from "@/lib/api";
import { formatarData } from "@/lib/format";
import { temPapel } from "@/lib/papeis";
import { cursosDe, MATERIAIS, materiaisService, type TipoMaterial } from "@/services/materiais";
import CapaMaterial from "./CapaMaterial";
import FormReserva from "./FormReserva";
import PaginaPrivada from "./PaginaPrivada";

export default function DetalheMaterial({ tipo, id }: { tipo: TipoMaterial; id: string }) {
  return (
    <PaginaPrivada>
      <Conteudo tipo={tipo} id={id} />
    </PaginaPrivada>
  );
}

function Conteudo({ tipo, id }: { tipo: TipoMaterial; id: string }) {
  const config = MATERIAIS[tipo];
  const { usuario } = useAuth();
  const { data: material, isPending, isError, error } = useMaterial(tipo, id);

  const voltar = (
    <Link href={config.rota} className="text-sm text-marca-600 hover:underline">
      ← Voltar para {config.rotulo.toLowerCase()}
    </Link>
  );

  if (isPending) return <p role="status">Carregando…</p>;

  if (isError) {
    const naoEncontrado = axios.isAxiosError(error) && error.response?.status === 404;
    return (
      <>
        {voltar}
        <p role="alert" className={`mt-3 rounded p-3 ${naoEncontrado ? "bg-yellow-50 text-yellow-900" : "bg-red-50 text-red-800"}`}>
          {naoEncontrado
            ? `${config.rotuloSingular} não encontrado. Ele pode ter sido removido ou ainda estar sendo indexado.`
            : mensagemDeErro(error, "Não foi possível carregar o item.")}
        </p>
      </>
    );
  }

  const cursos = cursosDe(material);
  const disponivel = (material.quantidade ?? 0) > 0;

  return (
    <>
      {voltar}
      <article className="mt-3 flex flex-col gap-6 rounded-lg bg-white p-5 shadow-sm md:flex-row">
        {config.temCapa && (
          <CapaMaterial tipo={tipo} id={material.id} titulo={material.titulo} className="h-72 w-48 shrink-0 self-start rounded" />
        )}
        <div className="flex-1 space-y-3">
          <h1 className="text-2xl font-bold text-marca-700">{material.titulo}</h1>
          <p className="text-gray-700">{material.autor}</p>
          <p className="text-sm text-gray-500">Publicação: {formatarData(material.dataPublicacao)}</p>
          <p className="whitespace-pre-line text-gray-800">{material.descricao}</p>

          <dl className="space-y-2 text-sm">
            <div>
              <dt className="font-semibold text-gray-700">{config.cursoUnico ? "Curso" : "Cursos"}</dt>
              <dd className="text-gray-600">{cursos.map((c) => c.nome).join(", ") || "—"}</dd>
            </div>
            <div>
              <dt className="font-semibold text-gray-700">Temas</dt>
              <dd className="text-gray-600">{material.temas.map((t) => t.nome).join(", ") || "—"}</dd>
            </div>
          </dl>

          {config.temQuantidade && (
            <p className={`font-medium ${disponivel ? "text-green-700" : "text-red-700"}`}>
              {disponivel ? `${material.quantidade} exemplar(es) disponível(is)` : "Nenhum exemplar disponível"}
            </p>
          )}

          {config.temConteudo && (
            <a
              href={materiaisService.urlConteudo(tipo, material.id)}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-block rounded bg-marca-700 px-4 py-2 font-semibold text-white hover:bg-marca-900"
            >
              Abrir PDF
            </a>
          )}
        </div>
      </article>

      {config.temQuantidade && temPapel(usuario?.role, "BIBLIOTECARIO") && (
        <FormReserva livroId={material.id} disponivel={disponivel} />
      )}
    </>
  );
}
