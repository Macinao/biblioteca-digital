package com.digital.smartteca.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * O MetodosMensagem original criava um WebClient novo a cada chamada —
 * um bean unico e injetado corrige isso (WebClient e projetado pra ser
 * construido uma vez e reutilizado; ele guarda um pool de conexoes por
 * baixo).
 */
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final EvolutionProperties evolutionProperties;

    @Bean
    public WebClient evolutionWebClient() {
        return WebClient.builder()
                .baseUrl(evolutionProperties.getUrl())
                .defaultHeader("apikey", evolutionProperties.getApikey())
                .build();
    }

}
