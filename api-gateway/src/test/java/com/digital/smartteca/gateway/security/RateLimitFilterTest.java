package com.digital.smartteca.gateway.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    RateLimitFilter filter;
    AtomicInteger encaminhadas;
    GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(objectMapper, 1);
        encaminhadas = new AtomicInteger();
        chain = exchange -> {
            encaminhadas.incrementAndGet();
            return Mono.empty();
        };
    }

    private MockServerWebExchange requisicao(String caminho, String ip) {
        return MockServerWebExchange.from(MockServerHttpRequest.post(caminho).header("X-Forwarded-For", ip));
    }

    private int dispararNaRota(String caminho, String ip, int vezes) {
        int bloqueadas = 0;
        for (int i = 0; i < vezes; i++) {
            MockServerWebExchange exchange = requisicao(caminho, ip);
            filter.filter(exchange, chain).block();
            if (exchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                bloqueadas++;
            }
        }
        return bloqueadas;
    }

    // ---------- login: 10/min ----------

    @Test
    void ateDezTentativasDeLoginPorMinutoPassam() {
        int bloqueadas = dispararNaRota("/api/v1/auth/login", "10.0.0.1", 10);

        assertThat(bloqueadas).isZero();
        assertThat(encaminhadas.get()).isEqualTo(10);
    }

    @Test
    void aDecimaPrimeiraTentativaDeLoginRetorna429ComRetryAfterEMensagemEmJson() throws Exception {
        dispararNaRota("/api/v1/auth/login", "10.0.0.1", 10);
        MockServerWebExchange exchange = requisicao("/api/v1/auth/login", "10.0.0.1");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
        assertThat(exchange.getResponse().getHeaders().getContentType().toString()).startsWith("application/json");
        JsonNode corpo = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertThat(corpo.get("statusCode").asInt()).isEqualTo(429);
        assertThat(corpo.get("message").asText()).isEqualTo("Muitas tentativas de login");
        assertThat(corpo.get("path").asText()).isEqualTo("/api/v1/auth/login");
        assertThat(encaminhadas.get()).isEqualTo(10);
    }

    @Test
    void cadaIpTemSeuProprioBalde() {
        dispararNaRota("/api/v1/auth/login", "10.0.0.1", 10);

        int bloqueadasDoOutroIp = dispararNaRota("/api/v1/auth/login", "10.0.0.2", 10);

        assertThat(bloqueadasDoOutroIp).isZero();
    }

    @Test
    void comUmProxyConfiavelVaiOUltimoIpDaCadeiaEOPrefixoForjadoNaoAjuda() {
        // O que o cliente escreve vem antes; o proxy confiável acrescenta, no fim, o IP real que viu.
        dispararNaRota("/api/v1/auth/login", "1.1.1.1, 10.0.0.1", 10);

        int comPrefixoNovo = dispararNaRota("/api/v1/auth/login", "9.9.9.9, 10.0.0.1", 1);

        assertThat(comPrefixoNovo).isEqualTo(1);
    }

    @Test
    void comDoisProxiesConfiaveisVaiAPenultimaEntrada() {
        RateLimitFilter doisProxies = new RateLimitFilter(objectMapper, 2);
        for (int i = 0; i < 10; i++) {
            doisProxies.filter(requisicao("/api/v1/auth/login", "forjado-" + i + ", 10.0.0.1, 172.16.0.5"), chain).block();
        }
        MockServerWebExchange onze = requisicao("/api/v1/auth/login", "outro-forjado, 10.0.0.1, 172.16.0.9");

        doisProxies.filter(onze, chain).block();

        assertThat(onze.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void semProxyConfiavelOXForwardedForEIgnoradoEVaiOEnderecoDaConexao() {
        RateLimitFilter exposto = new RateLimitFilter(objectMapper, 0);
        InetSocketAddress atacante = new InetSocketAddress("203.0.113.7", 5000);
        for (int i = 0; i < 10; i++) {
            exposto.filter(MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "ip-forjado-" + i).remoteAddress(atacante)), chain).block();
        }
        MockServerWebExchange onze = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                .header("X-Forwarded-For", "mais-um-forjado").remoteAddress(atacante));

        exposto.filter(onze, chain).block();

        assertThat(onze.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void cadeiaMaisCurtaQueOsProxiesConfiguradosCaiNoEnderecoDaConexao() {
        RateLimitFilter doisProxies = new RateLimitFilter(objectMapper, 2);
        InetSocketAddress remoto = new InetSocketAddress("203.0.113.7", 5000);
        for (int i = 0; i < 10; i++) {
            doisProxies.filter(MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                    .header("X-Forwarded-For", "so-um-" + i).remoteAddress(remoto)), chain).block();
        }
        MockServerWebExchange onze = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                .header("X-Forwarded-For", "so-um-novo").remoteAddress(remoto));

        doisProxies.filter(onze, chain).block();

        assertThat(onze.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void semXForwardedForUsaOEnderecoRemoto() {
        for (int i = 0; i < 10; i++) {
            filter.filter(MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                    .remoteAddress(new InetSocketAddress("203.0.113.7", 5000))), chain).block();
        }
        MockServerWebExchange onze = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("203.0.113.7", 5001)));

        filter.filter(onze, chain).block();

        assertThat(onze.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ---------- chatbot: 20/min ----------

    @Test
    void chatbotAceitaVinteMensagensPorMinutoEBloqueiaAVigesimaPrimeira() {
        int primeiras = dispararNaRota("/api/v1/chatbot/mensagem", "10.0.0.1", 20);
        MockServerWebExchange vigesimaPrimeira = requisicao("/api/v1/chatbot/mensagem", "10.0.0.1");

        filter.filter(vigesimaPrimeira, chain).block();

        assertThat(primeiras).isZero();
        assertThat(vigesimaPrimeira.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void oBaldeDoLoginEODoChatbotSaoIndependentes() {
        dispararNaRota("/api/v1/auth/login", "10.0.0.1", 10);

        int bloqueadasNoChatbot = dispararNaRota("/api/v1/chatbot/mensagem", "10.0.0.1", 5);

        assertThat(bloqueadasNoChatbot).isZero();
    }

    @Test
    void mensagemDoChatbotTrazTextoPropriaNoErro() throws Exception {
        dispararNaRota("/api/v1/chatbot/mensagem", "10.0.0.1", 20);
        MockServerWebExchange exchange = requisicao("/api/v1/chatbot/mensagem", "10.0.0.1");

        filter.filter(exchange, chain).block();

        assertThat(objectMapper.readTree(exchange.getResponse().getBodyAsString().block()).get("message").asText())
                .isEqualTo("Muitas mensagens enviadas ao chatbot");
    }

    // ---------- demais rotas ----------

    @Test
    void outrasRotasNuncaSaoLimitadas() {
        int bloqueadas = dispararNaRota("/api/v1/livro/listar", "10.0.0.1", 200);

        assertThat(bloqueadas).isZero();
        assertThat(encaminhadas.get()).isEqualTo(200);
    }

    @Test
    void rateLimitRodaAntesDoFiltroDeJwt() {
        assertThat(filter.getOrder()).isLessThan(new JwtAuthenticationFilter().getOrder());
    }
}
