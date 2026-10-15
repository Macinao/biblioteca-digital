package com.digital.smartteca.chatbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mesma situacao dos outros servicos nesta versao do Spring Boot — bean
 * explicito porque a autoconfiguracao nao se ativa sozinha aqui.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
