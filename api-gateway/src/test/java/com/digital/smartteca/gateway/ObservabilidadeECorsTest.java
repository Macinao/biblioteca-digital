package com.digital.smartteca.gateway;

import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.gateway.config.CorsConfig;
import com.digital.smartteca.gateway.observability.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ObservabilidadeECorsTest {

    // ---------- CorrelationIdFilter ----------

    @Test
    void geraUmCorrelationIdQuandoOClienteNaoMandaERepassaNaRequisicaoENaResposta() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/livro/listar"));
        AtomicReference<ServerWebExchange> repassada = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            repassada.set(ex);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();
        exchange.getResponse().setComplete().block();

        String noDestino = repassada.get().getRequest().getHeaders().getFirst(CorrelationId.HEADER);
        assertThat(UUID.fromString(noDestino)).isNotNull();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationId.HEADER)).isEqualTo(noDestino);
    }

    @Test
    void mantemOCorrelationIdEnviadoPeloCliente() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/x").header(CorrelationId.HEADER, "corr-do-cliente"));
        AtomicReference<ServerWebExchange> repassada = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            repassada.set(ex);
            return Mono.empty();
        }).block();
        exchange.getResponse().setComplete().block();

        assertThat(repassada.get().getRequest().getHeaders().get(CorrelationId.HEADER)).containsExactly("corr-do-cliente");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationId.HEADER)).isEqualTo("corr-do-cliente");
    }

    @Test
    void correlationIdEmBrancoEhSubstituido() {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/x").header(CorrelationId.HEADER, "  "));

        filter.filter(exchange, ex -> Mono.empty()).block();
        exchange.getResponse().setComplete().block();

        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationId.HEADER)).isNotBlank();
    }

    @Test
    void correlationIdEOPrimeiroFiltroDaCadeia() {
        assertThat(new CorrelationIdFilter().getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    // ---------- CorsConfig ----------

    private CorsWebFilter cors(String origens) {
        CorsConfig config = new CorsConfig();
        ReflectionTestUtils.setField(config, "allowedOrigins", origens);
        return config.corsWebFilter();
    }

    @Test
    void preflightDeOrigemPermitidaRecebeOsHeadersDeCorsComCredenciais() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("http://gateway.test:8080/api/v1/livro/listar")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"));
        AtomicReference<Boolean> chegouNaCadeia = new AtomicReference<>(false);

        cors("http://localhost:3000").filter(exchange, ex -> {
            chegouNaCadeia.set(true);
            return Mono.empty();
        }).block();

        HttpHeaders headers = exchange.getResponse().getHeaders();
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("http://localhost:3000");
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true");
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST", "DELETE", "PUT");
        assertThat(headers.getFirst(HttpHeaders.ACCESS_CONTROL_MAX_AGE)).isEqualTo("3600");
        assertThat(chegouNaCadeia.get()).as("preflight nao chega nos filtros de JWT").isFalse();
    }

    @Test
    void origemNaoPermitidaNaoRecebeHeadersDeCors() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("http://gateway.test:8080/api/v1/livro/listar")
                .header(HttpHeaders.ORIGIN, "http://site-malicioso.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        cors("http://localhost:3000").filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void variasOrigensSeparadasPorVirgulaSaoAceitas() {
        for (String origem : new String[]{"http://localhost:3000", "https://smartteca.com.br"}) {
            MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.options("http://gateway.test:8080/x")
                    .header(HttpHeaders.ORIGIN, origem)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"));

            cors("http://localhost:3000,https://smartteca.com.br").filter(exchange, ex -> Mono.empty()).block();

            assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo(origem);
        }
    }

    @Test
    void respostaDeRequisicaoRealExpoeOSetCookieEOTotalDeItensAoNavegador() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, "http://gateway.test:8080/api/v1/livro/listar")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000"));

        cors("http://localhost:3000").filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).contains("Set-Cookie").contains("X-Total-Count");
    }
}
