package com.digital.smartteca.chatbot.security;

import com.digital.smartteca.common.observability.CorrelationId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Primeiro filtro da cadeia (ver SecurityConfig): poe no MDC o id que o
 * api-gateway gerou/repassou, pra todo log desta requisicao (inclusive os
 * das tools do Spring AI, chamadas na mesma thread) carregar o mesmo id.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try (var ignored = CorrelationId.open(request.getHeader(CorrelationId.HEADER))) {
            response.setHeader(CorrelationId.HEADER, MDC.get(CorrelationId.MDC_KEY));
            filterChain.doFilter(request, response);
        }
    }
}
