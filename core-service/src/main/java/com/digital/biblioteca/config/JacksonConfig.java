package com.digital.biblioteca.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bean explicito porque, nesta versao do Spring Boot (4.1), a
 * autoconfiguracao padrao do Jackson simplesmente nao se ativa sozinha —
 * confirmado em praticamente todos os servicos deste monorepo,
 * independente da combinacao de starters presentes. O OutboxRelay
 * (serializando o envelope DomainEvent, que tem um campo Instant)
 * depende desse bean.
 *
 * Como criamos o ObjectMapper na mao (em vez de deixar a autoconfiguracao
 * do Boot montar), precisamos registrar o JavaTimeModule manualmente —
 * sem ele, serializar java.time.Instant lanca InvalidDefinitionException.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

}
