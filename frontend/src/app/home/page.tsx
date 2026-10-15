"use client";

import Link from "next/link";
import PaginaPrivada from "@/components/PaginaPrivada";
import { useAuth } from "@/hooks/useAuth";
import { temPapel } from "@/lib/papeis";
import type { Papel } from "@/types";

const ATALHOS: { href: string; titulo: string; descricao: string; papelMinimo: Papel }[] = [
  { href: "/livros", titulo: "Livros", descricao: "Acervo físico e disponibilidade.", papelMinimo: "ALUNO" },
  { href: "/livros-publicos", titulo: "Livros públicos", descricao: "Leitura digital em PDF.", papelMinimo: "ALUNO" },
  { href: "/tcc", titulo: "TCCs", descricao: "Trabalhos de conclusão de curso.", papelMinimo: "ALUNO" },
  { href: "/material-estudo", titulo: "Materiais de estudo", descricao: "Conteúdo dos professores.", papelMinimo: "ALUNO" },
  { href: "/reservas", titulo: "Minhas reservas", descricao: "Prazos e devoluções.", papelMinimo: "ALUNO" },
  { href: "/meus-materiais", titulo: "Meus materiais", descricao: "Publique e edite seus materiais.", papelMinimo: "PROFESSOR" },
  { href: "/gerenciar", titulo: "Gerenciar", descricao: "Acervo, reservas e usuários.", papelMinimo: "BIBLIOTECARIO" },
];

export default function Home() {
  const { usuario } = useAuth();

  return (
    <PaginaPrivada>
      <h1 className="text-2xl font-bold text-marca-700">Olá, {usuario?.nome.split(" ")[0]}!</h1>
      <p className="mt-1 text-gray-600">O que você quer fazer hoje?</p>

      <ul className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {ATALHOS.filter((a) => temPapel(usuario?.role, a.papelMinimo)).map((a) => (
          <li key={a.href}>
            <Link
              href={a.href}
              className="block h-full rounded-lg border border-gray-200 bg-white p-4 shadow-sm hover:border-marca-600 hover:shadow"
            >
              <span className="block font-semibold text-marca-700">{a.titulo}</span>
              <span className="block text-sm text-gray-600">{a.descricao}</span>
            </Link>
          </li>
        ))}
      </ul>
    </PaginaPrivada>
  );
}
