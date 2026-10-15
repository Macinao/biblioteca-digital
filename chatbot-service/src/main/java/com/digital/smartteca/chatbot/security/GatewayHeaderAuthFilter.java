package com.digital.smartteca.chatbot.security;

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
 * Mesmo padrao do search-service: confia nos headers que o api-gateway
 * ja validou, sem tabela de Usuario aqui (chatbot-service nao e dono
 * desse dado). O principal vira a propria matricula (String).
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
