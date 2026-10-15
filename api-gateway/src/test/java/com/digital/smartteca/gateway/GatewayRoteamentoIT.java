package com.digital.smartteca.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

/** Gateway de verdade (Netty + Spring Cloud Gateway) com servicos de destino falsos, em HTTP real. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoteamentoIT {

    record Chamada(String metodo, String caminhoComQuery, Map<String, List<String>> headers) {
        String header(String nome) {
            return headers.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(nome))
                    .findFirst().map(e -> e.getValue().get(0)).orElse(null);
        }
    }

    static class Destino {
        final HttpServer server;
        final String nome;
        final ConcurrentLinkedQueue<Chamada> chamadas = new ConcurrentLinkedQueue<>();

        Destino(String nome) {
            this.nome = nome;
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            server.createContext("/", exchange -> {
                Map<String, List<String>> headers = new java.util.HashMap<>(exchange.getRequestHeaders());
                String uri = exchange.getRequestURI().toString();
                chamadas.add(new Chamada(exchange.getRequestMethod(), uri, headers));
                // Os servicos reais tambem devolvem o X-Correlation-Id; o gateway nao pode duplicar o header.
                String correlationId = exchange.getRequestHeaders().getFirst("X-Correlation-Id");
                if (correlationId != null) {
                    exchange.getResponseHeaders().add("X-Correlation-Id", correlationId);
                }
                byte[] corpo = ("destino:" + nome).getBytes(StandardCharsets.UTF_8);
                if (uri.startsWith("/api/v1/auth/login")) {
                    exchange.getResponseHeaders().add("Set-Cookie", "smartteca_access=tok; Path=/; HttpOnly; SameSite=Lax");
                }
                exchange.sendResponseHeaders(200, corpo.length);
                exchange.getResponseBody().write(corpo);
                exchange.close();
            });
            server.start();
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }
    }

    static final TestKeys KEYS = new TestKeys();
    static final Destino CORE = new Destino("core");
    static final Destino SEARCH = new Destino("search");
    static final Destino STORAGE = new Destino("storage");
    static final Destino CHATBOT = new Destino("chatbot");

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.keys.jwt.public-key", KEYS::publicPem);
        registry.add("services.core-url", CORE::url);
        registry.add("services.search-url", SEARCH::url);
        registry.add("services.storage-url", STORAGE::url);
        registry.add("services.chatbot-url", CHATBOT::url);
        registry.add("spring.security.cors.allowed-origins", () -> "http://localhost:3000");
        // Os testes simulam clientes distintos pelo X-Forwarded-For, como faria um proxy confiável na frente.
        registry.add("app.rate-limit.proxies-confiaveis", () -> "1");
    }

    @LocalServerPort int porta;
    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
        for (Destino d : List.of(CORE, SEARCH, STORAGE, CHATBOT)) {
            d.chamadas.clear();
        }
    }

    private String ipUnico() {
        return "198.51.100." + (1 + Math.abs(UUID.randomUUID().hashCode() % 250)) + "-" + UUID.randomUUID();
    }

    private WebTestClient.RequestHeadersSpec<?> autenticado(WebTestClient.RequestHeadersUriSpec<?> metodo, String caminho, String roles) {
        return metodo.uri(caminho)
                .cookie("smartteca_access", KEYS.accessToken("2023001", roles))
                .header("X-Forwarded-For", ipUnico());
    }

    // ---------- roteamento ----------

    @Test
    void detalheDeUmItemPorIdVaiParaOSearchMasUsuarioMeEAuthContinuamNoCore() {
        for (String caminho : List.of("/api/v1/livro/abc", "/api/v1/livro-publico/abc", "/api/v1/tcc/abc", "/api/v1/material-estudo/abc")) {
            autenticado(client.get(), caminho, "ALUNO").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("destino:search");
        }
        autenticado(client.get(), "/api/v1/usuario/me", "ALUNO").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("destino:core");

        assertThat(SEARCH.chamadas).hasSize(4);
        assertThat(CORE.chamadas).extracting(Chamada::caminhoComQuery).containsExactly("/api/v1/usuario/me");
    }

    @Test
    void deleteDeUmItemPorIdContinuaNoCoreEOGetSoVaiParaOSearchNaListagemOuNoDetalhe() {
        autenticado(client.delete(), "/api/v1/livro/deletar/abc", "BIBLIOTECARIO").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("destino:core");
        autenticado(client.get(), "/api/v1/livro/baixar/capa/abc", "ALUNO").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("destino:storage");

        assertThat(SEARCH.chamadas).isEmpty();
    }

    @Test
    void rotasDeLeituraVaoParaOSearchService() {
        for (String caminho : List.of("/api/v1/livro/listar", "/api/v1/livro/buscar?texto=clean", "/api/v1/reserva/listar-minhas",
                "/api/v1/tcc/listar", "/api/v1/curso/listar")) {
            autenticado(client.get(), caminho, "ALUNO").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("destino:search");
        }

        assertThat(SEARCH.chamadas).extracting(Chamada::caminhoComQuery)
                .containsExactly("/api/v1/livro/listar", "/api/v1/livro/buscar?texto=clean", "/api/v1/reserva/listar-minhas",
                        "/api/v1/tcc/listar", "/api/v1/curso/listar");
        assertThat(CORE.chamadas).isEmpty();
    }

    @Test
    void rotasDeDownloadVaoParaOStorageService() {
        autenticado(client.get(), "/api/v1/livro-publico/baixar/abc", "ALUNO").exchange()
                .expectStatus().isOk().expectBody(String.class).isEqualTo("destino:storage");

        assertThat(STORAGE.chamadas).extracting(Chamada::caminhoComQuery).containsExactly("/api/v1/livro-publico/baixar/abc");
        assertThat(CORE.chamadas).isEmpty();
    }

    @Test
    void rotasDoChatbotVaoParaOChatbotService() {
        autenticado(client.post(), "/api/v1/chatbot/mensagem", "ALUNO").exchange()
                .expectStatus().isOk().expectBody(String.class).isEqualTo("destino:chatbot");

        assertThat(CHATBOT.chamadas).hasSize(1);
        assertThat(CORE.chamadas).isEmpty();
    }

    @Test
    void escritasEDemaisRotasDaApiVaoParaOCoreService() {
        autenticado(client.post(), "/api/v1/livro/cadastrar", "BIBLIOTECARIO").exchange()
                .expectStatus().isOk().expectBody(String.class).isEqualTo("destino:core");
        autenticado(client.delete(), "/api/v1/curso/deletar/abc", "BIBLIOTECARIO").exchange().expectStatus().isOk();
        autenticado(client.get(), "/api/v1/usuario/me", "ALUNO").exchange().expectStatus().isOk();

        assertThat(CORE.chamadas).extracting(Chamada::caminhoComQuery)
                .containsExactly("/api/v1/livro/cadastrar", "/api/v1/curso/deletar/abc", "/api/v1/usuario/me");
        assertThat(SEARCH.chamadas).isEmpty();
    }

    @Test
    void caminhoForaDeApiV1NaoTemRota() {
        client.get().uri("/qualquer-coisa").header("X-Forwarded-For", ipUnico()).exchange().expectStatus().isNotFound();

        assertThat(CORE.chamadas).isEmpty();
    }

    // ---------- autenticacao e identidade ----------

    @Test
    void semTokenRetorna401ENadaChegaAoDestino() {
        client.get().uri("/api/v1/livro/listar").header("X-Forwarded-For", ipUnico()).exchange().expectStatus().isUnauthorized();
        client.post().uri("/api/v1/livro/cadastrar").header("X-Forwarded-For", ipUnico()).exchange().expectStatus().isUnauthorized();

        assertThat(SEARCH.chamadas).isEmpty();
        assertThat(CORE.chamadas).isEmpty();
    }

    @Test
    void tokenValidoChegaAoDestinoComMatriculaERoleNosHeadersInternos() {
        autenticado(client.get(), "/api/v1/livro/listar", "PROFESSOR").exchange().expectStatus().isOk();

        Chamada chamada = SEARCH.chamadas.peek();
        assertThat(chamada.header("X-User-Matricula")).isEqualTo("2023001");
        assertThat(chamada.header("X-User-Roles")).isEqualTo("PROFESSOR,ALUNO");
    }

    @Test
    void bibliotecarioEProfessorChegamAoDestinoComTodosOsPapeisDaHierarquia() {
        autenticado(client.get(), "/api/v1/livro/listar", "BIBLIOTECARIO").exchange().expectStatus().isOk();
        autenticado(client.get(), "/api/v1/livro/listar", "PROFESSOR").exchange().expectStatus().isOk();
        autenticado(client.get(), "/api/v1/livro/listar", "ALUNO").exchange().expectStatus().isOk();

        var roles = SEARCH.chamadas.stream().map(c -> c.header("X-User-Roles")).toList();
        assertThat(roles).containsExactly("BIBLIOTECARIO,PROFESSOR,ALUNO", "PROFESSOR,ALUNO", "ALUNO");
    }

    @Test
    void refreshTokenNoLugarDoAccessRetorna401() {
        client.get().uri("/api/v1/livro/listar")
                .cookie("smartteca_access", KEYS.refreshToken("2023001"))
                .header("X-Forwarded-For", ipUnico()).exchange().expectStatus().isUnauthorized();

        assertThat(SEARCH.chamadas).isEmpty();
    }

    @Test
    void clienteNaoConsegueSeFazerPassarPorOutroUsuarioComHeadersForjados() {
        client.get().uri("/api/v1/livro/listar")
                .cookie("smartteca_access", KEYS.accessToken("2023001", "ALUNO"))
                .header("X-User-Matricula", "ADMIN").header("X-User-Roles", "BIBLIOTECARIO")
                .header("X-Forwarded-For", ipUnico()).exchange().expectStatus().isOk();

        Chamada chamada = SEARCH.chamadas.peek();
        assertThat(chamada.headers.get("X-user-matricula")).containsExactly("2023001");
        assertThat(chamada.headers.get("X-user-roles")).containsExactly("ALUNO");
    }

    @Test
    void headersForjadosEmRotaPublicaTambemNaoChegamAoCore() {
        client.post().uri("/api/v1/auth/login")
                .header("X-User-Matricula", "ADMIN").header("X-User-Roles", "BIBLIOTECARIO")
                .header("X-Forwarded-For", ipUnico()).exchange().expectStatus().isOk();

        Chamada chamada = CORE.chamadas.peek();
        assertThat(chamada.header("X-User-Matricula")).isNull();
        assertThat(chamada.header("X-User-Roles")).isNull();
    }

    @Test
    void loginEPublicoEOSetCookieDoDestinoVoltaAoCliente() {
        client.post().uri("/api/v1/auth/login").header("X-Forwarded-For", ipUnico()).exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches("Set-Cookie", "smartteca_access=tok.*HttpOnly.*");

        assertThat(CORE.chamadas).hasSize(1);
    }

    // ---------- correlation id ----------

    @Test
    void correlationIdEGeradoPropagadoAoDestinoEDevolvidoNaResposta() {
        var resposta = client.get().uri("/api/v1/livro/listar")
                .cookie("smartteca_access", KEYS.accessToken("2023001", "ALUNO"))
                .header("X-Forwarded-For", ipUnico())
                .exchange().expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id").returnResult(String.class);

        String naResposta = resposta.getResponseHeaders().getFirst("X-Correlation-Id");
        assertThat(SEARCH.chamadas.peek().header("X-Correlation-Id")).isEqualTo(naResposta);
    }

    @Test
    void correlationIdNaRespostaApareceUmaVezSoMesmoQuandoODestinoTambemODevolve() {
        var resposta = client.get().uri("/api/v1/livro/listar")
                .cookie("smartteca_access", KEYS.accessToken("2023001", "ALUNO"))
                .header("X-Forwarded-For", ipUnico())
                .exchange().expectStatus().isOk().returnResult(String.class);

        assertThat(resposta.getResponseHeaders().get("X-Correlation-Id")).hasSize(1);
    }

    @Test
    void correlationIdDoClienteEPreservadoDePontaAPonta() {
        client.get().uri("/api/v1/livro/listar")
                .cookie("smartteca_access", KEYS.accessToken("2023001", "ALUNO"))
                .header("X-Correlation-Id", "meu-id-123").header("X-Forwarded-For", ipUnico())
                .exchange().expectHeader().valueEquals("X-Correlation-Id", "meu-id-123");

        assertThat(SEARCH.chamadas.peek().header("X-Correlation-Id")).isEqualTo("meu-id-123");
    }

    // ---------- rate limit ----------

    @Test
    void decimaPrimeiraTentativaDeLoginDoMesmoIpRetorna429ENaoChegaAoCore() {
        String ip = ipUnico();
        for (int i = 0; i < 10; i++) {
            client.post().uri("/api/v1/auth/login").header("X-Forwarded-For", ip).exchange().expectStatus().isOk();
        }

        client.post().uri("/api/v1/auth/login").header("X-Forwarded-For", ip).exchange()
                .expectStatus().isEqualTo(429)
                .expectHeader().valueEquals("Retry-After", "60")
                .expectBody().jsonPath("$.message").isEqualTo("Muitas tentativas de login");

        assertThat(CORE.chamadas).hasSize(10);
    }

    // ---------- cors ----------

    @Test
    void preflightDeOrigemPermitidaRespondeSemPassarPeloJwtNemChegarAoDestino() {
        client.options().uri("/api/v1/livro/cadastrar")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("X-Forwarded-For", ipUnico())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:3000")
                .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");

        assertThat(CORE.chamadas).isEmpty();
    }

    @Test
    void preflightDeOrigemDesconhecidaNaoRecebeAutorizacaoDeCors() {
        client.options().uri("/api/v1/livro/cadastrar")
                .header("Origin", "http://site-malicioso.com")
                .header("Access-Control-Request-Method", "POST")
                .header("X-Forwarded-For", ipUnico())
                .exchange()
                .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    }

    // ---------- actuator ----------

    @Test
    void healthDoGatewayEPublico() {
        client.get().uri("/actuator/health").header("X-Forwarded-For", ipUnico()).exchange().expectStatus().isOk();
    }
}
