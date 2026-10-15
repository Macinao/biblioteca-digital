package com.digital.smartteca.gateway.security;

import com.digital.smartteca.gateway.treatment.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Portado do core-service nesta fase: antes rodava (potencialmente) em
 * cada servico de destino, agora roda uma unica vez, no unico ponto de
 * entrada. Em memoria mesmo (sem Redis) — consistente com a decisao de
 * single-replica deste estagio do projeto, documentada como trade-off
 * consciente no README.
 *
 * getOrder() menor que o do JwtAuthenticationFilter (-1): o rate limit
 * de login precisa valer mesmo pra tentativas com credencial invalida,
 * antes de qualquer verificacao de token.
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String ROTA_LOGIN = "/api/v1/auth/login";
    private static final String ROTA_CHATBOT = "/api/v1/chatbot/mensagem";
    private static final int LIMITE_LOGIN_POR_MINUTO = 10;
    private static final int LIMITE_CHATBOT_POR_MINUTO = 20;

    private final ObjectMapper objectMapper;
    private final int proxiesConfiaveis;
    private final ConcurrentHashMap<String, Bucket> bucketsLogin = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> bucketsChatbot = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper,
                           @Value("${app.rate-limit.proxies-confiaveis:0}") int proxiesConfiaveis) {
        this.objectMapper = objectMapper;
        this.proxiesConfiaveis = proxiesConfiaveis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();
        String ip = extrairIp(exchange);

        if (ROTA_LOGIN.equals(path) && !consumir(bucketsLogin, ip, LIMITE_LOGIN_POR_MINUTO)) {
            return limiteExcedido(exchange, "Muitas tentativas de login",
                    "Aguarde 1 minuto antes de tentar novamente.");
        }

        if (ROTA_CHATBOT.equals(path) && !consumir(bucketsChatbot, ip, LIMITE_CHATBOT_POR_MINUTO)) {
            return limiteExcedido(exchange, "Muitas mensagens enviadas ao chatbot",
                    "Aguarde 1 minuto antes de enviar novas mensagens.");
        }

        return chain.filter(exchange);
    }

    private boolean consumir(Map<String, Bucket> buckets, String ip, int limite) {
        Bucket bucket = buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limite)
                        .refillGreedy(limite, Duration.ofMinutes(1))
                        .build())
                .build());
        return bucket.tryConsume(1);
    }

    private Mono<Void> limiteExcedido(ServerWebExchange exchange, String mensagem, String detalhes) {

        String path = exchange.getRequest().getURI().getPath();
        log.warn("Rate limit excedido em {}. IP: {}", path, extrairIp(exchange));

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", "60");

        ApiError erro = ApiError.of(HttpStatus.TOO_MANY_REQUESTS, mensagem, detalhes, path);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(erro);
        } catch (Exception e) {
            bytes = ("{\"message\":\"" + mensagem + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * O X-Forwarded-For só é confiável a partir de proxies que o próprio operador colocou na frente do
     * gateway: cada um acrescenta ao FIM da lista o endereço de quem falou com ele. Tudo que vier antes
     * disso foi escrito pelo cliente e pode ser forjado para escapar do limite (um IP novo por tentativa).
     * Por isso, com N proxies confiáveis, vale a N-ésima entrada contando do fim; com 0 (padrão, gateway
     * exposto direto) o header é ignorado e vale o endereço da conexão TCP.
     */
    private String extrairIp(ServerWebExchange exchange) {
        if (proxiesConfiaveis > 0) {
            List<String> linhas = exchange.getRequest().getHeaders().get("X-Forwarded-For");
            if (linhas != null) {
                String[] cadeia = String.join(",", linhas).split(",");
                int indice = cadeia.length - proxiesConfiaveis;
                if (indice >= 0 && !cadeia[indice].isBlank()) {
                    return cadeia[indice].trim();
                }
            }
        }
        var remoteAddress = exchange.getRequest().getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "desconhecido";
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
