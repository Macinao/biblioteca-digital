"use client";

import Link from "next/link";
import PaginaPrivada from "@/components/PaginaPrivada";

const SECOES = [
  { href: "/gerenciar/livros", titulo: "Livros", descricao: "Acervo físico e exemplares." },
  { href: "/gerenciar/livros-publicos", titulo: "Livros públicos", descricao: "Obras digitais em PDF." },
  { href: "/gerenciar/tcc", titulo: "TCCs", descricao: "Trabalhos de conclusão." },
  { href: "/gerenciar/material-estudo", titulo: "Materiais de estudo", descricao: "Conteúdos dos professores." },
  { href: "/gerenciar/cursos", titulo: "Cursos", descricao: "Cursos da instituição." },
  { href: "/gerenciar/temas", titulo: "Temas", descricao: "Assuntos para classificar o acervo." },
  { href: "/gerenciar/reservas", titulo: "Reservas", descricao: "Empréstimos e devoluções." },
  { href: "/gerenciar/usuarios", titulo: "Usuários", descricao: "Cadastrar alunos, professores e bibliotecários." },
];

export default function Gerenciar() {
  return (
    <PaginaPrivada papelMinimo="BIBLIOTECARIO">
      <h1 className="text-2xl font-bold text-marca-700">Gerenciar</h1>
      <ul className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {SECOES.map((s) => (
          <li key={s.href}>
            <Link href={s.href} className="block h-full rounded-lg border border-gray-200 bg-white p-4 shadow-sm hover:border-marca-600 hover:shadow">
              <span className="block font-semibold text-marca-700">{s.titulo}</span>
              <span className="block text-sm text-gray-600">{s.descricao}</span>
            </Link>
          </li>
        ))}
      </ul>
    </PaginaPrivada>
  );
}
