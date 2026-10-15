package com.digital.smartteca.chatbot.service;

import com.digital.smartteca.chatbot.ai.SmartBot;
import com.digital.smartteca.chatbot.memoria.MemoriaService;
import com.digital.smartteca.chatbot.memoria.Mensagem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Portado do core-service quase 1:1. A diferenca: o chatbot-service nao
 * tem a entidade Usuario (nao e dono desse dado) — chega aqui so a
 * matricula, que o GatewayHeaderAuthFilter extraiu do header confiavel.
 * Ela vira tanto a chave da memoria no Redis quanto o "nome" usado no
 * prompt (simplificacao consciente: mostrar a matricula em vez do nome
 * de exibicao, pra nao precisar de mais uma chamada de rede so pra
 * buscar esse dado no core-service).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private static final int LIMITE_HISTORICO = 10;

    private final SmartBot bot;
    private final MemoriaService memoriaService;

    @Override
    public String chatbotConversa(String conversa, String matricula) {

        List<Mensagem> recentes = memoriaService.buscarUltimasMensagens(matricula, LIMITE_HISTORICO);

        String historico = recentes.isEmpty()
                ? "usuario: Nenhuma conversa anterior. \n assistente: Nenhuma conversa anterior."
                : recentes
                .stream()
                .map(msg -> msg.getRole() + ": " + msg.getConteudo())
                .collect(Collectors.joining("\n"));

        String resposta = chatComRetry(matricula, historico, conversa);

        memoriaService.adicionarMensagem(matricula, "usuario", conversa);
        memoriaService.adicionarMensagem(matricula, "assistente", resposta);

        return resposta;
    }

    /**
     * O tool-calling de modelos menores (rodando local via Ollama, ou cloud) às vezes gera a
     * chamada de função em formato inválido (erro "tool_use_failed"), de forma intermitente.
     * Como é aleatório, uma nova tentativa costuma resolver; se persistir, devolve uma mensagem
     * amigável em vez de derrubar como 500.
     */
    private String chatComRetry(String nome, String historico, String conversa) {
        try {
            return bot.chat(nome, historico, conversa);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("tool_use_failed")) {
                log.warn("Chatbot: tool_use_failed do modelo — tentando novamente uma vez.");
                try {
                    return bot.chat(nome, historico, conversa);
                } catch (RuntimeException retry) {
                    log.warn("Chatbot: tool_use_failed persistiu após o retry.", retry);
                    return "Desculpe, tive um problema técnico ao processar sua pergunta. Pode tentar novamente?";
                }
            }
            throw e;
        }
    }
}
