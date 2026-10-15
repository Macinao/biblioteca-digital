"use client";

import { useEffect, useRef, useState } from "react";
import { mensagemDeErro } from "@/lib/api";
import { chatbotService } from "@/services/chatbot";
import { PREFIXO_CONVERSA } from "./conversaAssistente";

interface Mensagem {
  id: number;
  autor: "usuario" | "bot";
  texto: string;
  erro?: boolean;
}

const BOAS_VINDAS: Mensagem = {
  id: 0,
  autor: "bot",
  texto: "Olá! Sou o assistente da SmartTeca. Posso ajudar a encontrar livros, livros públicos, TCCs e materiais de estudo no acervo.",
};

const MAX_MENSAGENS_GUARDADAS = 50;

function lerConversa(chave: string): Mensagem[] {
  try {
    const guardado = JSON.parse(sessionStorage.getItem(chave) ?? "null");
    if (Array.isArray(guardado) && guardado.length > 0) return guardado;
  } catch {
    // sessionStorage indisponível ou conteúdo corrompido: começa uma conversa nova
  }
  return [BOAS_VINDAS];
}

/** **negrito** vira <strong>; tudo mais é texto puro (React escapa), então a resposta do modelo nunca injeta HTML. */
function Linha({ texto }: { texto: string }) {
  return (
    <>
      {texto.split(/(\*\*[^*]+\*\*)/g).map((parte, i) =>
        parte.startsWith("**") && parte.endsWith("**") ? <strong key={i}>{parte.slice(2, -2)}</strong> : <span key={i}>{parte}</span>,
      )}
    </>
  );
}

export function TextoDoBot({ texto }: { texto: string }) {
  return (
    <div className="space-y-1">
      {texto.split("\n").map((linha, i) => {
        const item = linha.match(/^\s*[-*]\s+(.*)$/);
        if (item) {
          return (
            <p key={i} className="flex gap-2">
              <span aria-hidden="true">•</span>
              <span><Linha texto={item[1]} /></span>
            </p>
          );
        }
        return linha.trim() === "" ? null : <p key={i}><Linha texto={linha} /></p>;
      })}
    </div>
  );
}

export default function Chatbot({ matricula }: { matricula: string }) {
  const chave = PREFIXO_CONVERSA + matricula;
  const [aberto, setAberto] = useState(false);
  const [texto, setTexto] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [mensagens, setMensagens] = useState<Mensagem[]>(() => lerConversa(chave));
  const proximoId = useRef(Math.max(...mensagens.map((m) => m.id)) + 1);
  const fim = useRef<HTMLDivElement>(null);

  useEffect(() => {
    try {
      sessionStorage.setItem(chave, JSON.stringify(mensagens.slice(-MAX_MENSAGENS_GUARDADAS)));
    } catch {
      // cota cheia ou modo privado: a conversa só não sobrevive ao recarregamento
    }
  }, [chave, mensagens]);

  useEffect(() => {
    fim.current?.scrollIntoView?.({ block: "end" });
  }, [mensagens, aberto]);

  async function enviar(e: React.FormEvent) {
    e.preventDefault();
    const pergunta = texto.trim();
    if (!pergunta || enviando) return;

    setMensagens((m) => [...m, { id: proximoId.current++, autor: "usuario", texto: pergunta }]);
    setTexto("");
    setEnviando(true);
    try {
      const resposta = await chatbotService.enviar(pergunta);
      setMensagens((m) => [...m, { id: proximoId.current++, autor: "bot", texto: resposta }]);
    } catch (erro) {
      setMensagens((m) => [
        ...m,
        { id: proximoId.current++, autor: "bot", erro: true, texto: mensagemDeErro(erro, "O assistente não está disponível agora. Tente de novo em instantes.") },
      ]);
    } finally {
      setEnviando(false);
    }
  }

  if (!aberto) {
    return (
      <button
        type="button"
        onClick={() => setAberto(true)}
        className="fixed right-4 bottom-4 rounded-full bg-marca-700 px-5 py-3 font-semibold text-white shadow-lg hover:bg-marca-900"
      >
        Assistente
      </button>
    );
  }

  return (
    <section aria-label="Assistente virtual" className="fixed right-4 bottom-4 flex h-[28rem] w-80 max-w-[calc(100vw-2rem)] flex-col overflow-hidden rounded-lg border border-gray-200 bg-white shadow-xl">
      <header className="flex items-center justify-between bg-marca-700 px-4 py-2 text-white">
        <h2 className="font-semibold">Assistente SmartTeca</h2>
        <button type="button" onClick={() => setAberto(false)} aria-label="Fechar assistente" className="px-2 text-xl leading-none hover:bg-white/10">
          ×
        </button>
      </header>

      <div className="flex-1 space-y-3 overflow-y-auto p-3 text-sm" role="log" aria-live="polite">
        {mensagens.map((m) => (
          <div key={m.id} className={m.autor === "usuario" ? "flex justify-end" : "flex justify-start"}>
            <div
              className={`max-w-[85%] rounded-lg px-3 py-2 ${
                m.autor === "usuario" ? "bg-marca-700 text-white" : m.erro ? "bg-red-50 text-red-800" : "bg-gray-100 text-gray-900"
              }`}
            >
              {m.autor === "bot" ? <TextoDoBot texto={m.texto} /> : m.texto}
            </div>
          </div>
        ))}
        {enviando && <p className="text-xs text-gray-500">Digitando…</p>}
        <div ref={fim} />
      </div>

      <form onSubmit={enviar} className="flex gap-2 border-t border-gray-200 p-2">
        <label htmlFor="mensagem-chat" className="sr-only">Mensagem</label>
        <input
          id="mensagem-chat"
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          maxLength={1000}
          placeholder="Pergunte algo…"
          className="min-w-0 flex-1 rounded border border-gray-300 px-2 py-1.5"
        />
        <button type="submit" disabled={enviando || texto.trim() === ""} className="rounded bg-marca-700 px-3 py-1.5 font-semibold text-white disabled:opacity-50">
          Enviar
        </button>
      </form>
    </section>
  );
}
