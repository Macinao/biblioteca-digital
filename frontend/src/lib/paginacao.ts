/** Uma página de resultados e o total de itens (vem no header X-Total-Count, que o gateway expõe via CORS). */
export interface Pagina<T> {
  itens: T[];
  total: number;
}

/** Lê o total dos cabeçalhos da resposta; sem o header, o total é o que veio. */
export function lerTotal(cabecalhos: Record<string, unknown>, itens: unknown[]): number {
  const total = Number(cabecalhos["x-total-count"]);
  return Number.isFinite(total) ? total : itens.length;
}

/** Quantas páginas existem para `total` itens (no mínimo 1, para a tela dizer "página 1 de 1"). */
export function totalDePaginas(total: number, tamanho: number): number {
  return Math.max(1, Math.ceil(total / tamanho));
}
