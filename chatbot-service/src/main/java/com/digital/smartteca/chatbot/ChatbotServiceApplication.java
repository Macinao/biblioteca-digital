package com.digital.smartteca.chatbot;

import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// As autoconfiguracoes de audio/imagem/embedding/moderacao do Spring AI 1.1.4
// quebram na inicializacao com o Spring Boot 4.1 (chamam um metodo de
// HttpHeaders que foi removido nessa versao do Boot). O projeto so usa
// chat com tools (function calling), entao desligamos o resto explicitamente.
//
// UserDetailsServiceAutoConfiguration excluida pelo mesmo motivo do
// search-service/storage-service: o GatewayHeaderAuthFilter monta a
// autenticacao a partir dos headers do gateway, nunca usa o
// AuthenticationManager/UserDetailsService padrao.
@SpringBootApplication(exclude = {
        OpenAiAudioSpeechAutoConfiguration.class,
        OpenAiAudioTranscriptionAutoConfiguration.class,
        OpenAiEmbeddingAutoConfiguration.class,
        OpenAiImageAutoConfiguration.class,
        OpenAiModerationAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
public class ChatbotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotServiceApplication.class, args);
    }

}
