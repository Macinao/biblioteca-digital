package com.digital.smartteca.gateway.observability;

import com.digital.smartteca.common.observability.CorrelationId;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Roda antes de qualquer outro filtro (rate limit, JWT): todo request que
 * entra pelo gateway sai com um X-Correlation-Id, gerado aqui se o
 * cliente nao mandou um. Os servicos de destino so precisam ler o header
 * e botar no MDC — a geracao do id fica concentrada no unico ponto de
 * entrada.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String correlationId = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CorrelationId.HEADER, correlationId)
                .build();

        // beforeCommit + set: os servicos de destino tambem devolvem o header, e o gateway copia esses
        // headers para a resposta. Definir so na hora de enviar substitui o valor em vez de duplica-lo.
        final String id = correlationId;
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(CorrelationId.HEADER, id);
            return Mono.empty();
        });

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
