package com.digital.smartteca.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

/**
 * Tabela de roteamento do gateway. A ordem importa: rotas mais especificas
 * (leitura/download/chat) vem antes da rota "pega tudo" do core-service,
 * porque o Spring Cloud Gateway usa a primeira rota que casar com o caminho.
 */
@Configuration
public class RouteConfig {

    @Value("${services.core-url}")
    private String coreUrl;

    @Value("${services.search-url}")
    private String searchUrl;

    @Value("${services.storage-url}")
    private String storageUrl;

    @Value("${services.chatbot-url}")
    private String chatbotUrl;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("chatbot", r -> r.path("/api/v1/chatbot/**").uri(chatbotUrl))
                .route("storage-download", r -> r.path("/api/v1/*/baixar/**").uri(storageUrl))
                .route("search-read", r -> r.path(
                                "/api/v1/*/listar",
                                "/api/v1/*/listar-minhas",
                                "/api/v1/*/buscar")
                        .uri(searchUrl))
                // GET de um item pelo id. Só os quatro tipos de material, e só GET: um padrão genérico
                // (/api/v1/*/*) capturaria /api/v1/usuario/me e /api/v1/auth/login, que são do core-service.
                .route("search-detalhe", r -> r.method(HttpMethod.GET).and().path(
                                "/api/v1/livro/*",
                                "/api/v1/livro-publico/*",
                                "/api/v1/tcc/*",
                                "/api/v1/material-estudo/*")
                        .uri(searchUrl))
                .route("core-service", r -> r.path("/api/v1/**").uri(coreUrl))
                .build();
    }
}
