package com.digital.biblioteca.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.digital.biblioteca.treatment.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String ROTA_LOGIN = "/api/v1/auth/login";
    private static final String ROTA_CHATBOT = "/api/v1/chatbot/mensagem";
    private static final int LIMITE_LOGIN_POR_MINUTO = 10;
    private static final int LIMITE_CHATBOT_POR_MINUTO = 20;

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Bucket> bucketsLogin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> bucketsChatbot = new ConcurrentHashMap<>();

    private Bucket bucketDoIp(Map<String, Bucket> buckets, String ip, int limite) {
        return buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.simple(limite, Duration.ofMinutes(1)))
                .build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String ip = extrairIp(request);

        if (ROTA_LOGIN.equals(uri)) {
            if (!consumir(bucketsLogin, ip, LIMITE_LOGIN_POR_MINUTO)) {
                responderLimiteExcedido(response, request, "Muitas tentativas de login",
                        "Aguarde 1 minuto antes de tentar novamente.", ip);
                return;
            }
        } else if (ROTA_CHATBOT.equals(uri)) {
            if (!consumir(bucketsChatbot, ip, LIMITE_CHATBOT_POR_MINUTO)) {
                responderLimiteExcedido(response, request, "Muitas mensagens enviadas ao chatbot",
                        "Aguarde 1 minuto antes de enviar novas mensagens.", ip);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean consumir(Map<String, Bucket> buckets, String ip, int limite) {
        return bucketDoIp(buckets, ip, limite).tryConsume(1);
    }

    private void responderLimiteExcedido(HttpServletResponse response,
                                         HttpServletRequest request,
                                         String mensagem,
                                         String detalhes,
                                         String ip) throws IOException {

        log.warn("Rate limit excedido em {}. IP: {}", request.getRequestURI(), ip);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", "60");

        ApiError erro = ApiError.of(
                HttpStatus.TOO_MANY_REQUESTS,
                mensagem,
                detalhes,
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), erro);
    }

    private String extrairIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
