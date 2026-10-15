import { api, API_URL } from "@/lib/api";
import { lerTotal, type Pagina } from "@/lib/paginacao";
import type { Curso, Material, Papel, Tema } from "@/types";

export type TipoMaterial = "livro" | "livro-publico" | "tcc" | "material-estudo";

/**
 * Os quatro tipos de material têm a mesma "forma" no front (listar, buscar, detalhar, baixar arquivo);
 * o que muda vem desta tabela. Assim uma tela serve para os quatro, em vez de quatro cópias.
 */
export interface ConfigMaterial {
  tipo: TipoMaterial;
  /** Prefixo da API: /livro, /livro-publico, ... */
  caminho: string;
  rotulo: string;
  rotuloSingular: string;
  /** Rota das telas de leitura (/livros, /livros-publicos, ...). */
  rota: string;
  rotaGerenciar: string;
  temCapa: boolean;
  temConteudo: boolean;
  temQuantidade: boolean;
  /** TCC pertence a um curso só; os demais a vários. */
  cursoUnico: boolean;
  /** Nome da parte JSON no multipart do cadastro (o backend exige exatamente estes nomes). */
  parteJson: string;
  papelEscrita: Papel;
}

export const MATERIAIS: Record<TipoMaterial, ConfigMaterial> = {
  livro: {
    tipo: "livro",
    caminho: "/livro",
    rotulo: "Livros",
    rotuloSingular: "Livro",
    rota: "/livros",
    rotaGerenciar: "/gerenciar/livros",
    temCapa: true,
    temConteudo: false,
    temQuantidade: true,
    cursoUnico: false,
    parteJson: "livro",
    papelEscrita: "BIBLIOTECARIO",
  },
  "livro-publico": {
    tipo: "livro-publico",
    caminho: "/livro-publico",
    rotulo: "Livros públicos",
    rotuloSingular: "Livro público",
    rota: "/livros-publicos",
    rotaGerenciar: "/gerenciar/livros-publicos",
    temCapa: true,
    temConteudo: true,
    temQuantidade: false,
    cursoUnico: false,
    parteJson: "livro-publico",
    papelEscrita: "BIBLIOTECARIO",
  },
  tcc: {
    tipo: "tcc",
    caminho: "/tcc",
    rotulo: "TCCs",
    rotuloSingular: "TCC",
    rota: "/tcc",
    rotaGerenciar: "/gerenciar/tcc",
    temCapa: false,
    temConteudo: true,
    temQuantidade: false,
    cursoUnico: true,
    parteJson: "tcc",
    papelEscrita: "BIBLIOTECARIO",
  },
  "material-estudo": {
    tipo: "material-estudo",
    caminho: "/material-estudo",
    rotulo: "Materiais de estudo",
    rotuloSingular: "Material de estudo",
    rota: "/material-estudo",
    rotaGerenciar: "/gerenciar/material-estudo",
    temCapa: false,
    temConteudo: true,
    temQuantidade: false,
    cursoUnico: false,
    parteJson: "material-estudo",
    papelEscrita: "PROFESSOR",
  },
};

export const TIPOS_MATERIAL = Object.keys(MATERIAIS) as TipoMaterial[];

/** Cursos do material, seja ele de um curso só (TCC) ou de vários. */
export function cursosDe(material: Material): Curso[] {
  return material.cursos ?? (material.curso ? [material.curso] : []);
}

/** O que o formulário envia; ids de curso e tema (não objetos), como a API espera. */
export interface DadosMaterial {
  titulo: string;
  autor: string;
  descricao: string;
  dataPublicacao: string; // LocalDateTime: "2020-05-01T00:00:00"
  temas: string[];
  cursos?: string[];
  curso?: string; // só TCC
  quantidade?: number; // só livro
}

export interface ArquivosMaterial {
  capa?: File | null;
  conteudo?: File | null;
}

/**
 * Multipart no formato do backend: uma parte JSON (com o nome do tipo e Content-Type application/json,
 * senão o Spring não a lê como objeto) mais as partes de arquivo.
 */
function montarFormulario(config: ConfigMaterial, dados: object, arquivos: ArquivosMaterial): FormData {
  const form = new FormData();
  form.append(config.parteJson, new Blob([JSON.stringify(dados)], { type: "application/json" }));
  if (arquivos.capa) form.append("capa", arquivos.capa);
  if (arquivos.conteudo) form.append("conteudo", arquivos.conteudo);
  return form;
}

export interface ParametrosPagina {
  pagina: number; // começa em 0
  tamanho: number;
  cursoId?: string;
}

export const materiaisService = {
  async criar(tipo: TipoMaterial, dados: DadosMaterial, arquivos: ArquivosMaterial): Promise<Material> {
    const config = MATERIAIS[tipo];
    const { data } = await api.post<Material>(`${config.caminho}/cadastrar`, montarFormulario(config, dados, arquivos));
    return data;
  },

  /** Arquivos são opcionais na edição: sem arquivo novo, o anterior permanece. */
  async atualizar(tipo: TipoMaterial, id: string, dados: DadosMaterial, arquivos: ArquivosMaterial): Promise<Material> {
    const config = MATERIAIS[tipo];
    const { data } = await api.put<Material>(
      `${config.caminho}/atualizar`,
      montarFormulario(config, { id, ...dados }, arquivos),
    );
    return data;
  },

  async remover(tipo: TipoMaterial, id: string): Promise<void> {
    await api.delete(`${MATERIAIS[tipo].caminho}/deletar/${id}`);
  },

  /** Uma página do acervo (mais recentes primeiro), opcionalmente só de um curso. O filtro roda no servidor. */
  async listarPagina(tipo: TipoMaterial, { pagina, tamanho, cursoId }: ParametrosPagina): Promise<Pagina<Material>> {
    const resposta = await api.get<Material[]>(`${MATERIAIS[tipo].caminho}/listar`, {
      params: { pagina, tamanho, cursoId: cursoId || undefined },
    });
    return { itens: resposta.data, total: lerTotal(resposta.headers, resposta.data) };
  },

  async obter(tipo: TipoMaterial, id: string): Promise<Material> {
    const { data } = await api.get<Material>(`${MATERIAIS[tipo].caminho}/${id}`);
    return data;
  },

  /** Busca textual no Elasticsearch (analisador em português: "algoritmo" acha "Algoritmos"). */
  async buscar(tipo: TipoMaterial, texto: string): Promise<Material[]> {
    const { data } = await api.get<Material[]>(`${MATERIAIS[tipo].caminho}/buscar`, { params: { texto } });
    return data;
  },

  /** URL direta de arquivo: o navegador a abre/baixa mandando o cookie de sessão sozinho. */
  urlCapa(tipo: TipoMaterial, id: string): string {
    return `${API_URL}${MATERIAIS[tipo].caminho}/baixar/capa/${id}`;
  },

  urlConteudo(tipo: TipoMaterial, id: string): string {
    return `${API_URL}${MATERIAIS[tipo].caminho}/baixar/conteudo/${id}`;
  },
};

export type TipoCatalogo = "curso" | "tema";

/** Curso e tema são iguais (só um nome), mas o backend nomeia os campos de atualização de cada um. */
export const CATALOGOS: Record<TipoCatalogo, { caminho: string; rotulo: string; rotuloSingular: string; campoId: string; campoNome: string }> = {
  curso: { caminho: "/curso", rotulo: "Cursos", rotuloSingular: "Curso", campoId: "cursoId", campoNome: "novoCurso" },
  tema: { caminho: "/tema", rotulo: "Temas", rotuloSingular: "Tema", campoId: "temaId", campoNome: "novoTema" },
};

export const catalogosService = {
  async cursos(): Promise<Curso[]> {
    const { data } = await api.get<Curso[]>("/curso/listar");
    return data;
  },

  async temas(): Promise<Tema[]> {
    const { data } = await api.get<Tema[]>("/tema/listar");
    return data;
  },

  async criar(tipo: TipoCatalogo, nome: string): Promise<Curso | Tema> {
    const { data } = await api.post<Curso | Tema>(`${CATALOGOS[tipo].caminho}/cadastrar`, { nome });
    return data;
  },

  async renomear(tipo: TipoCatalogo, id: string, nome: string): Promise<Curso | Tema> {
    const c = CATALOGOS[tipo];
    const { data } = await api.put<Curso | Tema>(`${c.caminho}/atualizar`, { [c.campoId]: id, [c.campoNome]: nome });
    return data;
  },

  async remover(tipo: TipoCatalogo, id: string): Promise<void> {
    await api.delete(`${CATALOGOS[tipo].caminho}/deletar/${id}`);
  },
};
