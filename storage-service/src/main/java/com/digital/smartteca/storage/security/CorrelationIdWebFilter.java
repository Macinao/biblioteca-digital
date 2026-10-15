package com.digital.smartteca.storage.security;

import com.digital.smartteca.common.observability.CorrelationId;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Equivalente reativo do CorrelationIdFilter dos servicos servlet. Em
 * WebFlux nao da pra so chamar MDC.put antes do chain.filter(...) — a
 * cadeia pode continuar em outra thread do event loop, e o MDC e
 * ThreadLocal. Por isso o id vai no Context do Reactor; o Spring Boot
 * propaga isso pro MDC automaticamente onde o log realmente acontece
 * (via micrometer context-propagation, ja trazido pelo starter Actuator).
 */
@Component
public class CorrelationIdWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String valor = CorrelationId.resolve(
                exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER));

        exchange.getResponse().getHeaders().set(CorrelationId.HEADER, valor);

        return chain.filter(exchange)
                .contextWrite(Context.of(CorrelationId.MDC_KEY, valor));
    }
}
