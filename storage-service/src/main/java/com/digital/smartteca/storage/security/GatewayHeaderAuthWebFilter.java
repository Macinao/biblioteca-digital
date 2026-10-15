package com.digital.smartteca.storage.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Versao reativa do GatewayHeaderAuthFilter do search-service — mesma
 * ideia (confia nos headers do gateway, sem tabela de Usuario aqui), so
 * que o storage-service e WebFlux (nao servlet), entao o mecanismo de
 * injetar a autenticacao no contexto e diferente: em vez de
 * SecurityContextHolder (ThreadLocal, nao funciona com Reactor), usa-se
 * ReactiveSecurityContextHolder.withAuthentication(...), que propaga a
 * autenticacao pelo Context reativo da cadeia de processamento.
 */
@Component
public class GatewayHeaderAuthWebFilter implements WebFilter {

    private static final String HEADER_MATRICULA = "X-User-Matricula";
    private static final String HEADER_ROLES = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String matricula = exchange.getRequest().getHeaders().getFirst(HEADER_MATRICULA);
        String roles = exchange.getRequest().getHeaders().getFirst(HEADER_ROLES);

        if (matricula == null || matricula.isBlank() || roles == null || roles.isBlank()) {
            return chain.filter(exchange);
        }

        List<GrantedAuthority> authorities = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        Authentication auth = new UsernamePasswordAuthenticationToken(matricula, null, authorities);

        return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

    }
}
