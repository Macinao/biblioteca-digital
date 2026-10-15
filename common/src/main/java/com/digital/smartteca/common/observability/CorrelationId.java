package com.digital.smartteca.common.observability;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Nao e tracing distribuido de verdade (Zipkin/Jaeger fica documentado
 * como proximo passo no README) — e so um id gerado uma vez no
 * api-gateway (unico ponto de entrada) e repassado como header em toda
 * chamada subsequente, entre servicos e em mensagens Kafka/RabbitMQ. Cada
 * servico poe esse valor no MDC do SLF4J, o que já basta pra filtrar
 * "todo log desta requisicao" numa ferramenta de log agregado e cruzar
 * com o que apareceu nas metricas do Prometheus no mesmo intervalo.
 */
public final class CorrelationId {

    private CorrelationId() {
    }

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    /**
     * Gera um novo UUID se vier nulo/vazio (evento sem origem HTTP, ex.:
     * disparado por @Scheduled, ou primeiro hop sem o header ainda).
     */
    public static String resolve(String correlationId) {
        return (correlationId == null || correlationId.isBlank())
                ? UUID.randomUUID().toString()
                : correlationId;
    }

    /**
     * Poe o id no MDC do SLF4J e devolve algo fechavel num try-with-resources
     * — todo log dentro do escopo carrega o id, e ele sai do MDC sozinho no
     * fim (importante em consumer de fila: a thread e reaproveitada pra
     * proxima mensagem, entao nao pode vazar o id de uma pra outra). Uso
     * pensado pra codigo imperativo (servlet, consumer); em WebFlux, poe o
     * id no Context do Reactor via contextWrite(...) em vez de chamar isto.
     */
    public static Scope open(String correlationId) {
        String valor = resolve(correlationId);
        MDC.put(MDC_KEY, valor);
        return () -> MDC.remove(MDC_KEY);
    }

    /**
     * AutoCloseable sem "throws Exception" no close() — evita forcar todo
     * call site (incluindo filtros servlet, que so declaram
     * ServletException/IOException) a lidar com uma checked exception que
     * o close() aqui nunca de fato lanca.
     */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
