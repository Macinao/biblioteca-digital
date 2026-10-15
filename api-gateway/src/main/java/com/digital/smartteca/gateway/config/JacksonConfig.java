package com.digital.smartteca.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mesmo gap de autoconfiguracao do Jackson visto em praticamente todo
 * servico deste monorepo (Spring Boot 4.1 + esta combinacao de starters
 * nao ativa o ObjectMapper sozinho). RateLimitFilter depende deste bean
 * pra serializar o corpo de erro 429.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

}
