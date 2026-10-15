package com.digital.smartteca.storage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mesma situacao dos outros servicos: essa versao do Spring Boot nao
 * disponibiliza um ObjectMapper sozinha aqui, e o StorageCommandConsumer
 * precisa de um pra desserializar UploadArquivoCommand/DeletarArquivoCommand.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
