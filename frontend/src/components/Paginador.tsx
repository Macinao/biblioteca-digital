"use client";

import { totalDePaginas } from "@/lib/paginacao";

interface Props {
  /** Página atual, começando em 0 (como a API). */
  pagina: number;
  total: number;
  tamanho: number;
  aoMudar: (pagina: number) => void;
  /** Congela os botões enquanto a nova página carrega. */
  ocupado?: boolean;
}

export default function Paginador({ pagina, total, tamanho, aoMudar, ocupado = false }: Props) {
  const paginas = totalDePaginas(total, tamanho);
  const botao = "rounded border border-gray-300 bg-white px-3 py-1.5 text-sm hover:bg-gray-50 disabled:opacity-50";

  return (
    <nav aria-label="Paginação" className="mt-4 flex items-center justify-between gap-3 text-sm">
      <button type="button" className={botao} disabled={pagina === 0 || ocupado} onClick={() => aoMudar(pagina - 1)}>
        Anterior
      </button>
      <span aria-live="polite">
        Página {pagina + 1} de {paginas} · {total} {total === 1 ? "item" : "itens"}
      </span>
      <button type="button" className={botao} disabled={pagina + 1 >= paginas || ocupado} onClick={() => aoMudar(pagina + 1)}>
        Próxima
      </button>
    </nav>
  );
}
