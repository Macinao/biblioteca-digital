package com.digital.smartteca.gateway.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.digital.smartteca.common.security.RotasPublicas;
import com.digital.smartteca.common.security.RsaKeyLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.stream.Stream;

/**
 * Unico lugar do sistema que valida o JWT de verdade. Roda antes do
 * roteamento (getOrder negativo): rota publica passa direto, rota
 * protegida sem token valido nunca chega a ser encaminhada (corta aqui,
 * com 401). Token valido vira dois headers internos (X-User-Matricula,
 * X-User-Roles) que os servicos de destino passam a confiar, em vez de
 * cada um validar o JWT de novo.
 *
 * So tem a chave PUBLICA (RsaKeyLoader com privateKey nulo) — este
 * servico consegue verificar assinatura, nunca criar um token novo.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String ISSUER_ACCESS = "smartteca-access";
    private static final String ACCESS_COOKIE = "smartteca_access";

    // Rotas de negocio compartilhadas (RotasPublicas.AUTH, do modulo common)
    // + endpoints de infraestrutura proprios deste servico.
    private static final List<String> PUBLIC_PATHS = Stream.concat(
            RotasPublicas.AUTH.stream(),
            Stream.of("/actuator/health", "/actuator/info", "/actuator/prometheus")
    ).toList();

    @Value("${spring.keys.jwt.public-key}")
    private String publicKeyPem;

    private Algorithm algorithm;

    @PostConstruct
    private void init() throws Exception {
        RSAPublicKey publicKey = RsaKeyLoader.loadPublicKey(publicKeyPem);
        this.algorithm = Algorithm.RSA256(publicKey, null);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (isPublic(path)) {
            // Identidade so existe se o gateway a atribuir: o que o cliente mandar nesses headers nunca passa adiante.
            ServerHttpRequest semIdentidade = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Matricula");
                        headers.remove("X-User-Roles");
                    })
                    .build();
            return chain.filter(exchange.mutate().request(semIdentidade).build());
        }

        String token = extractToken(exchange);
        if (token == null) {
            return unauthorized(exchange);
        }

        try {
            DecodedJWT jwt = JWT.require(algorithm)
                    .withIssuer(ISSUER_ACCESS)
                    .build()
                    .verify(token);

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Matricula", jwt.getSubject())
                    .header("X-User-Roles", expandirPapeis(jwt.getClaim("roles").asString()))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JWTVerificationException e) {
            return unauthorized(exchange);
        }
    }

    // Mesma hierarquia de Usuario.getAuthorities() no core-service: quem pode mais herda o que pode menos.
    // Search/storage/chatbot nao tem a tabela de usuarios e leem so este header (hasRole('ALUNO') etc.).
    private static String expandirPapeis(String papel) {
        if (papel == null) {
            return null;
        }
        return switch (papel) {
            case "BIBLIOTECARIO" -> "BIBLIOTECARIO,PROFESSOR,ALUNO";
            case "PROFESSOR" -> "PROFESSOR,ALUNO";
            default -> papel;
        };
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path);
    }

    private String extractToken(ServerWebExchange exchange) {
        HttpCookie cookie = exchange.getRequest().getCookies().getFirst(ACCESS_COOKIE);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
