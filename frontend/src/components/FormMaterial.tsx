"use client";

import { Controller, useForm } from "react-hook-form";
import { useCursos, useTemas } from "@/hooks/useCatalogo";
import { dataParaApi, dataParaInput } from "@/lib/format";
import { cursosDe, MATERIAIS, type ArquivosMaterial, type DadosMaterial, type TipoMaterial } from "@/services/materiais";
import type { Curso, Material, Tema } from "@/types";
import SelecaoMultipla from "./SelecaoMultipla";

const LIMITE_ARQUIVO_BYTES = 10 * 1024 * 1024; // mesmo teto do backend (spring.servlet.multipart.max-file-size)

interface Campos {
  titulo: string;
  autor: string;
  descricao: string;
  dataPublicacao: string;
  quantidade: number;
  cursos: string[];
  curso: string;
  temas: string[];
  capa: FileList;
  conteudo: FileList;
}

interface Props {
  tipo: TipoMaterial;
  /** Presente = edição (arquivos viram opcionais); ausente = cadastro. */
  material?: Material;
  salvando: boolean;
  erro?: string;
  aoSalvar: (dados: DadosMaterial, arquivos: ArquivosMaterial) => void;
  aoCancelar: () => void;
}

const classeCampo = "w-full rounded border border-gray-300 px-3 py-2";

export default function FormMaterial(props: Props) {
  const cursos = useCursos();
  const temas = useTemas();

  // O formulário só monta com as opções já carregadas: um <select> preenchido antes de existirem as <option>
  // perde o valor inicial (ao editar, o curso do item apareceria em branco).
  if (cursos.isError || temas.isError) {
    return (
      <p role="alert" className="rounded bg-red-50 p-3 text-red-800">
        Não foi possível carregar cursos e temas. Feche e tente novamente.
      </p>
    );
  }
  if (!cursos.data || !temas.data) return <p role="status">Carregando…</p>;

  return <Formulario {...props} cursos={cursos.data} temas={temas.data} />;
}

function Formulario({ tipo, material, salvando, erro, aoSalvar, aoCancelar, cursos, temas }: Props & { cursos: Curso[]; temas: Tema[] }) {
  const config = MATERIAIS[tipo];
  const edicao = Boolean(material);

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<Campos>({
    defaultValues: {
      titulo: material?.titulo ?? "",
      autor: material?.autor ?? "",
      descricao: material?.descricao ?? "",
      dataPublicacao: dataParaInput(material?.dataPublicacao),
      quantidade: material?.quantidade ?? 1,
      cursos: material ? cursosDe(material).map((c) => c.id) : [],
      curso: material?.curso?.id ?? "",
      temas: material?.temas.map((t) => t.id) ?? [],
    },
  });

  function aoEnviar(c: Campos) {
    const dados: DadosMaterial = {
      titulo: c.titulo.trim(),
      autor: c.autor.trim(),
      descricao: c.descricao.trim(),
      dataPublicacao: dataParaApi(c.dataPublicacao),
      temas: c.temas,
      ...(config.cursoUnico ? { curso: c.curso } : { cursos: c.cursos }),
      ...(config.temQuantidade ? { quantidade: Number(c.quantidade) } : {}),
    };
    aoSalvar(dados, { capa: c.capa?.[0] ?? null, conteudo: c.conteudo?.[0] ?? null });
  }

  const validarArquivo = (extensoes: string[], rotulo: string, obrigatorio: boolean) => (arquivos?: FileList) => {
    const arquivo = arquivos?.[0];
    if (!arquivo) return obrigatorio ? `Envie ${rotulo}.` : true;
    if (!extensoes.some((e) => arquivo.name.toLowerCase().endsWith(e))) return `Formato inválido (use ${extensoes.join(", ")}).`;
    if (arquivo.size > LIMITE_ARQUIVO_BYTES) return "O arquivo passa de 10 MB.";
    return true;
  };

  const exigirLista = (mensagem: string) => (v: string[]) => v.length > 0 || mensagem;

  return (
    <form onSubmit={handleSubmit(aoEnviar)} noValidate className="space-y-4">
      <div>
        <label htmlFor="titulo" className="mb-1 block text-sm font-medium text-gray-700">Título</label>
        <input id="titulo" className={classeCampo} {...register("titulo", { required: "Informe o título." })} />
        {errors.titulo && <p className="mt-1 text-sm text-red-700">{errors.titulo.message}</p>}
      </div>

      <div>
        <label htmlFor="autor" className="mb-1 block text-sm font-medium text-gray-700">Autor</label>
        <input id="autor" className={classeCampo} {...register("autor", { required: "Informe o autor." })} />
        {errors.autor && <p className="mt-1 text-sm text-red-700">{errors.autor.message}</p>}
      </div>

      <div>
        <label htmlFor="descricao" className="mb-1 block text-sm font-medium text-gray-700">Descrição</label>
        <textarea id="descricao" rows={3} className={classeCampo} {...register("descricao", { required: "Informe a descrição." })} />
        {errors.descricao && <p className="mt-1 text-sm text-red-700">{errors.descricao.message}</p>}
      </div>

      <div className="flex flex-wrap gap-4">
        <div>
          <label htmlFor="dataPublicacao" className="mb-1 block text-sm font-medium text-gray-700">Data de publicação</label>
          <input id="dataPublicacao" type="date" className={classeCampo} {...register("dataPublicacao", { required: "Informe a data." })} />
          {errors.dataPublicacao && <p className="mt-1 text-sm text-red-700">{errors.dataPublicacao.message}</p>}
        </div>
        {config.temQuantidade && (
          <div>
            <label htmlFor="quantidade" className="mb-1 block text-sm font-medium text-gray-700">Exemplares</label>
            <input
              id="quantidade"
              type="number"
              min={0}
              className={classeCampo}
              {...register("quantidade", { required: "Informe a quantidade.", min: { value: 0, message: "Não pode ser negativa." } })}
            />
            {errors.quantidade && <p className="mt-1 text-sm text-red-700">{errors.quantidade.message}</p>}
          </div>
        )}
      </div>

      {config.cursoUnico ? (
        <div>
          <label htmlFor="curso" className="mb-1 block text-sm font-medium text-gray-700">Curso</label>
          <select id="curso" className={classeCampo} {...register("curso", { required: "Escolha o curso." })}>
            <option value="">Selecione…</option>
            {cursos.map((c) => (
              <option key={c.id} value={c.id}>{c.nome}</option>
            ))}
          </select>
          {errors.curso && <p className="mt-1 text-sm text-red-700">{errors.curso.message}</p>}
        </div>
      ) : (
        <Controller
          name="cursos"
          control={control}
          rules={{ validate: exigirLista("Escolha ao menos um curso.") }}
          render={({ field, fieldState }) => (
            <SelecaoMultipla rotulo="Cursos" opcoes={cursos} valor={field.value} aoMudar={field.onChange} erro={fieldState.error?.message} />
          )}
        />
      )}

      <Controller
        name="temas"
        control={control}
        rules={{ validate: exigirLista("Escolha ao menos um tema.") }}
        render={({ field, fieldState }) => (
          <SelecaoMultipla rotulo="Temas" opcoes={temas} valor={field.value} aoMudar={field.onChange} erro={fieldState.error?.message} />
        )}
      />

      {config.temCapa && (
        <div>
          <label htmlFor="capa" className="mb-1 block text-sm font-medium text-gray-700">
            Capa (PNG, JPG ou WEBP){edicao && " — deixe vazio para manter a atual"}
          </label>
          <input id="capa" type="file" accept=".png,.jpg,.jpeg,.webp" {...register("capa", { validate: validarArquivo([".png", ".jpg", ".jpeg", ".webp"], "a capa", !edicao) })} />
          {errors.capa && <p className="mt-1 text-sm text-red-700">{errors.capa.message}</p>}
        </div>
      )}

      {config.temConteudo && (
        <div>
          <label htmlFor="conteudo" className="mb-1 block text-sm font-medium text-gray-700">
            Arquivo PDF{edicao && " — deixe vazio para manter o atual"}
          </label>
          <input id="conteudo" type="file" accept=".pdf" {...register("conteudo", { validate: validarArquivo([".pdf"], "o PDF", !edicao) })} />
          {errors.conteudo && <p className="mt-1 text-sm text-red-700">{errors.conteudo.message}</p>}
        </div>
      )}

      {erro && (
        <p role="alert" className="rounded bg-red-50 p-2 text-sm text-red-800">
          {erro}
        </p>
      )}

      <div className="flex justify-end gap-3">
        <button type="button" onClick={aoCancelar} className="rounded border border-gray-300 px-4 py-2 hover:bg-gray-50">
          Cancelar
        </button>
        <button type="submit" disabled={salvando} className="rounded bg-marca-700 px-4 py-2 font-semibold text-white hover:bg-marca-900 disabled:opacity-60">
          {salvando ? "Salvando…" : edicao ? "Salvar alterações" : "Cadastrar"}
        </button>
      </div>
    </form>
  );
}
