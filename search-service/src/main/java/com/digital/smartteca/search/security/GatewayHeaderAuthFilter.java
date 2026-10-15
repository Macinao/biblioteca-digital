package com.digital.smartteca.search.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Versao mais simples do GatewayAuthFilter do core-service. A diferenca:
 * o search-service nao tem a tabela de Usuario (nao e dono desse dado),
 * entao nao ha o que buscar no banco pra confirmar o papel de quem fez a
 * requisicao — confiamos direto no que o api-gateway ja validou e mandou
 * nos headers X-User-Matricula/X-User-Roles. A rede interna do Docker
 * garante que ninguem chega aqui sem passar pelo gateway primeiro.
 *
 * Isso significa que, ao contrario do core-service, revogar um papel so
 * tem efeito aqui quando o token expira (o gateway so re-verifica na
 * proxima emissao). Pra dado de leitura de Curso/Tema/Livro isso e uma
 * troca aceitavel — nao vale reintroduzir uma consulta a banco que este
 * servico nem deveria ter.
 */
@Component
public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_MATRICULA = "X-User-Matricula";
    private static final String HEADER_ROLES = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String matricula = request.getHeader(HEADER_MATRICULA);
        String roles = request.getHeader(HEADER_ROLES);

        if (matricula == null || matricula.isBlank() || roles == null || roles.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        List<GrantedAuthority> authorities = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(matricula, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);

    }
}
