/** A conversa fica na sessão do navegador (sessionStorage), uma por matrícula: sobrevive ao recarregar a página. */
export const PREFIXO_CONVERSA = "smartteca-chat:";

/** Apaga todas as conversas guardadas: chamado no logout, para a próxima pessoa não ver a conversa da anterior. */
export function limparConversasDoAssistente() {
  try {
    Object.keys(sessionStorage)
      .filter((chave) => chave.startsWith(PREFIXO_CONVERSA))
      .forEach((chave) => sessionStorage.removeItem(chave));
  } catch {
    // sessionStorage indisponível: não há o que limpar
  }
}
