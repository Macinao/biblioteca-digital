"use client";

import { useAuth } from "@/hooks/useAuth";
import Chatbot from "./Chatbot";

/**
 * O assistente vive aqui, acima das páginas, e não dentro de cada uma: o Next desmonta a página ao navegar,
 * e com ela o estado do chat (conversa e painel aberto). Neste ponto da árvore ele permanece montado.
 * O key faz a conversa começar do zero se outra pessoa entrar no mesmo navegador.
 */
export default function AssistenteGlobal() {
  const { usuario } = useAuth();
  return usuario ? <Chatbot key={usuario.matricula} matricula={usuario.matricula} /> : null;
}
