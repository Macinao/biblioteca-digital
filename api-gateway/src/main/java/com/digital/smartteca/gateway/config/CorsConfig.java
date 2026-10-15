package com.digital.smartteca.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Ate a Fase 6, CORS vivia so no core-service — o unico servico que o
 * browser falava diretamente. Com o gateway virando a porta unica de
 * entrada, o CORS migra pra ca; os demais servicos nunca mais recebem
 * requisicao de browser (so do gateway, dentro da rede Docker).
 *
 * CorsWebFilter e um WebFilter comum do WebFlux, nao um GlobalFilter do
 * Gateway — roda ANTES da cadeia de GlobalFilter (JwtAuthenticationFilter,
 * RateLimitFilter), o que importa pra requisicoes de preflight (OPTIONS):
 * elas nunca chegam a esbarrar no filtro de JWT.
 */
@Configuration
public class CorsConfig {

    @Value("${spring.security.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "X-Requested-With"
        ));

        // X-Total-Count: total de itens das listagens paginadas; sem expô-lo o JavaScript do navegador não consegue lê-lo.
        configuration.setExposedHeaders(List.of("Set-Cookie", "X-Total-Count"));

        configuration.setAllowCredentials(true);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return new CorsWebFilter(source);

    }
}
