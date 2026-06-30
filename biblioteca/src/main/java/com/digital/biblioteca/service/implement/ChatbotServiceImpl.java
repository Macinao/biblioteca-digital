package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.config.ai.Mensagem;
import com.digital.biblioteca.config.ai.SmartBot;
import com.digital.biblioteca.document.MemoriaDocument;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final int LIMITE_HISTORICO = 10;

    private final SmartBot bot;
    private final MemoriaServiceImpl memoriaService;

    @Override
    public String chatbotConversa(String conversa, Usuario usuario) {

        MemoriaDocument memoria = memoriaService.buscar(usuario.getId().toString());

        List<Mensagem> conversas = memoria.getConversas();
        List<Mensagem> recentes = conversas.size() <= LIMITE_HISTORICO
                ? conversas
                : conversas.subList(conversas.size() - LIMITE_HISTORICO, conversas.size());

        String historico = recentes.isEmpty()
                ? "usuario: Nenhuma conversa anterior. \n assistente: Nenhuma conversa anterior."
                : recentes
                .stream()
                .map(msg ->
                        msg.getRole() + ": " + msg.getConteudo()
                )
                .collect(Collectors.joining("\n"));

        String resposta = chatComRetry(
                usuario.getNome(),
                historico,
                conversa
        );

        memoriaService.adicionarMensagem(
                usuario.getId().toString(),
                "usuario",
                conversa
        );

        memoriaService.adicionarMensagem(
                usuario.getId().toString(),
                "assistente",
                resposta
        );

        return resposta;
    }

    /**
     * O tool-calling do Llama na Groq às vezes gera a chamada de função em formato inválido
     * (erro "tool_use_failed"), de forma intermitente. Como é aleatório, uma nova tentativa
     * costuma resolver; se persistir, devolve uma mensagem amigável em vez de derrubar como 500.
     */
    private String chatComRetry(String nome, String historico, String conversa) {
        try {
            return bot.chat(nome, historico, conversa);
        } catch (NonTransientAiException e) {
            if (e.getMessage() != null && e.getMessage().contains("tool_use_failed")) {
                log.warn("Chatbot: tool_use_failed do modelo — tentando novamente uma vez.");
                try {
                    return bot.chat(nome, historico, conversa);
                } catch (NonTransientAiException retry) {
                    log.warn("Chatbot: tool_use_failed persistiu após o retry.", retry);
                    return "Desculpe, tive um problema técnico ao processar sua pergunta. Pode tentar novamente?";
                }
            }
            throw e;
        }
    }
}
