"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { temPapel } from "@/lib/papeis";
import { ROTULO_PAPEL, type Papel } from "@/types";
import Logo from "./Logo";

interface ItemMenu {
  href: string;
  rotulo: string;
  papelMinimo: Papel;
}

const MENU: ItemMenu[] = [
  { href: "/livros", rotulo: "Livros", papelMinimo: "ALUNO" },
  { href: "/livros-publicos", rotulo: "Livros públicos", papelMinimo: "ALUNO" },
  { href: "/tcc", rotulo: "TCCs", papelMinimo: "ALUNO" },
  { href: "/material-estudo", rotulo: "Materiais de estudo", papelMinimo: "ALUNO" },
  { href: "/reservas", rotulo: "Minhas reservas", papelMinimo: "ALUNO" },
  { href: "/meus-materiais", rotulo: "Meus materiais", papelMinimo: "PROFESSOR" },
  { href: "/gerenciar", rotulo: "Gerenciar", papelMinimo: "BIBLIOTECARIO" },
];

export default function Cabecalho() {
  const { usuario, sair } = useAuth();
  const caminho = usePathname();
  const router = useRouter();

  async function aoSair() {
    await sair();
    router.replace("/");
  }

  return (
    <header className="bg-marca-700 text-white shadow">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3">
        <Link href="/home" aria-label="SmartTeca, ir para o início">
          <Logo />
        </Link>

        <nav aria-label="Principal" className="flex flex-wrap items-center gap-1 text-sm">
          {MENU.filter((item) => temPapel(usuario?.role, item.papelMinimo)).map((item) => {
            const ativo = caminho === item.href || caminho.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={ativo ? "page" : undefined}
                className={`rounded px-3 py-1.5 hover:bg-marca-600 ${ativo ? "bg-marca-600 font-semibold" : ""}`}
              >
                {item.rotulo}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-3 text-sm">
          <Link href="/perfil" className="text-right leading-tight hover:underline">
            <span className="block font-semibold">{usuario?.nome}</span>
            <span className="block text-xs text-white/70">{usuario ? ROTULO_PAPEL[usuario.role] : ""}</span>
          </Link>
          <button
            type="button"
            onClick={aoSair}
            className="rounded border border-white/40 px-3 py-1.5 hover:bg-white/10"
          >
            Sair
          </button>
        </div>
      </div>
    </header>
  );
}
