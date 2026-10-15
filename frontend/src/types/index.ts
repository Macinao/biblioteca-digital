export type Papel = "ALUNO" | "PROFESSOR" | "BIBLIOTECARIO";

export type StatusReserva = "RESERVADO" | "ATRASADO";

export interface Usuario {
  id: string;
  nome: string;
  matricula: string;
  email: string;
  telefone: string;
  role: Papel;
}

export interface Curso {
  id: string;
  nome: string;
}

export interface Tema {
  id: string;
  nome: string;
}

/** Campos comuns às quatro respostas de material (o que o search-service devolve). */
export interface Material {
  id: string;
  titulo: string;
  autor: string;
  descricao: string;
  temas: Tema[];
  dataPublicacao: string;
  cursos?: Curso[]; // livro, livro público e material de estudo
  curso?: Curso; // TCC tem um curso só
  quantidade?: number; // só livro físico
}

export interface Reserva {
  id: string;
  livroId: string;
  tituloLivro: string;
  autorLivro: string;
  matriculaUsuario: string;
  nomeUsuario: string;
  statusReserva: StatusReserva;
  dataCriacao: string;
  dataDevolucao: string;
}

export interface NovoUsuario {
  nome: string;
  matricula: string;
  senha: string;
  email: string;
  telefone: string;
  role: Papel;
}

export const ROTULO_PAPEL: Record<Papel, string> = {
  ALUNO: "Aluno",
  PROFESSOR: "Professor",
  BIBLIOTECARIO: "Bibliotecário",
};

export const LIMITE_RESERVAS_POR_USUARIO = 3;
