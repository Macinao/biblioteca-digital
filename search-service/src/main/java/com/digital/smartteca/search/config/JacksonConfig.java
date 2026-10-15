package com.digital.smartteca.search.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mesma situacao do core-service: nesta versao do Spring Boot a
 * autoconfiguracao padrao nao esta disponibilizando um ObjectMapper
 * sozinha, entao os consumers Kafka (que desserializam o envelope
 * DomainEvent, com um campo Instant) precisam de um bean explicito
 * com o JavaTimeModule registrado.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        // Leitor tolerante: os *Document sao duplicados entre core-service e search-service; um campo novo no
        // producer nao pode derrubar o consumo (e descartar a mensagem apos as retentativas do Kafka).
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

}
