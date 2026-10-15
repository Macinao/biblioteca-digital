package com.digital.smartteca.gateway.security;

import com.digital.smartteca.gateway.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    final TestKeys keys = new TestKeys();
    JwtAuthenticationFilter filter;
    AtomicReference<ServerWebExchange> repassada;
    GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "publicKeyPem", keys.publicPem());
        ReflectionTestUtils.invokeMethod(filter, "init");
        repassada = new AtomicReference<>();
        chain = exchange -> {
            repassada.set(exchange);
            return Mono.empty();
        };
    }

    private MockServerWebExchange get(String caminho) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(caminho));
    }

    private MockServerWebExchange comCookie(String caminho, String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(caminho).cookie(new HttpCookie("smartteca_access", token)));
    }

    // ---------- rotas publicas ----------

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
            "/actuator/health", "/actuator/info", "/actuator/prometheus"})
    void rotasPublicasPassamSemTokenENaoSaoAutenticadas(String caminho) {
        MockServerWebExchange exchange = get(caminho);

        filter.filter(exchange, chain).block();

        assertThat(repassada.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void cadastroDeUsuarioNaoEPublicoExigeToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/usuario"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repassada.get()).isNull();
    }

    @Test
    void rotaPublicaIgnoraTokenInvalidoPoisNaoPrecisaDele() {
        filter.filter(comCookie("/api/v1/auth/login", "lixo"), chain).block();

        assertThat(repassada.get()).isNotNull();
    }

    @Test
    void usuarioMeNaoEPublicoMesmoComPrefixoDeUmaRotaPublica() {
        MockServerWebExchange exchange = get("/api/v1/usuario/me");

        filter.filter(exchange, chain).block();

        assertThat(repassada.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ---------- rotas protegidas ----------

    @Test
    void semTokenRetorna401ENaoEncaminha() {
        MockServerWebExchange exchange = get("/api/v1/livro/listar");

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repassada.get()).isNull();
    }

    @Test
    void tokenValidoNoCookieViraOsDoisHeadersInternosDeIdentidade() {
        MockServerWebExchange exchange = comCookie("/api/v1/livro/listar", keys.accessToken("2023001", "PROFESSOR"));

        filter.filter(exchange, chain).block();

        var headers = repassada.get().getRequest().getHeaders();
        assertThat(headers.getFirst("X-User-Matricula")).isEqualTo("2023001");
        assertThat(headers.getFirst("X-User-Roles")).isEqualTo("PROFESSOR,ALUNO");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void tokenValidoNoHeaderAuthorizationBearerTambemFunciona() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/livro/listar")
                .header("Authorization", "Bearer " + keys.accessToken("2023001", "ALUNO")));

        filter.filter(exchange, chain).block();

        assertThat(repassada.get().getRequest().getHeaders().getFirst("X-User-Matricula")).isEqualTo("2023001");
    }

    @Test
    void cookieTemPrioridadeSobreOHeaderAuthorization() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/livro/listar")
                .cookie(new HttpCookie("smartteca_access", keys.accessToken("DO_COOKIE", "ALUNO")))
                .header("Authorization", "Bearer " + keys.accessToken("DO_HEADER", "ALUNO")));

        filter.filter(exchange, chain).block();

        assertThat(repassada.get().getRequest().getHeaders().getFirst("X-User-Matricula")).isEqualTo("DO_COOKIE");
    }

    @Test
    void authorizationQueNaoEBearerEIgnorado() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/livro/listar")
                .header("Authorization", "Basic abc"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cookieEmBrancoCaiParaOHeaderAuthorization() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/livro/listar")
                .cookie(new HttpCookie("smartteca_access", "   "))
                .header("Authorization", "Bearer " + keys.accessToken("2023001", "ALUNO")));

        filter.filter(exchange, chain).block();

        assertThat(repassada.get().getRequest().getHeaders().getFirst("X-User-Matricula")).isEqualTo("2023001");
    }

    @Test
    void tokenExpiradoRetorna401() {
        MockServerWebExchange exchange = comCookie("/api/v1/livro/listar",
                keys.accessToken("2023001", "ALUNO", Instant.now().minusSeconds(60)));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repassada.get()).isNull();
    }

    @Test
    void tokenAssinadoPorOutraChaveRetorna401() {
        String forjado = keys.tokenAssinadoPorOutraChave("2023001", "BIBLIOTECARIO");

        MockServerWebExchange exchange = comCookie("/api/v1/livro/listar", forjado);
        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repassada.get()).isNull();
    }

    @Test
    void refreshTokenNaoVaiComoAccessToken() {
        MockServerWebExchange exchange = comCookie("/api/v1/livro/listar", keys.refreshToken("2023001"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(repassada.get()).isNull();
    }

    @Test
    void tokenAdulteradoOuLixoRetorna401() {
        String token = keys.accessToken("2023001", "ALUNO");
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        for (String invalido : new String[]{adulterado, "lixo", "a.b.c"}) {
            MockServerWebExchange exchange = comCookie("/api/v1/livro/listar", invalido);
            filter.filter(exchange, chain).block();
            assertThat(exchange.getResponse().getStatusCode()).as(invalido).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(repassada.get()).isNull();
    }

    // ---------- anti-spoofing ----------

    @Test
    void headersDeIdentidadeEnviadosPeloClienteSaoSubstituidosPelosDoToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/livro/listar")
                .cookie(new HttpCookie("smartteca_access", keys.accessToken("2023001", "ALUNO")))
                .header("X-User-Matricula", "ADMIN")
                .header("X-User-Roles", "BIBLIOTECARIO"));

        filter.filter(exchange, chain).block();

        var headers = repassada.get().getRequest().getHeaders();
        assertThat(headers.get("X-User-Matricula")).containsExactly("2023001");
        assertThat(headers.get("X-User-Roles")).containsExactly("ALUNO");
    }

    @Test
    void headersDeIdentidadeDoClienteNaoChegamAoDestinoNemEmRotaPublica() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/login")
                .header("X-User-Matricula", "ADMIN")
                .header("X-User-Roles", "BIBLIOTECARIO"));

        filter.filter(exchange, chain).block();

        var headers = repassada.get().getRequest().getHeaders();
        assertThat(headers.containsHeader("X-User-Matricula")).isFalse();
        assertThat(headers.containsHeader("X-User-Roles")).isFalse();
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"BIBLIOTECARIO,BIBLIOTECARIO;PROFESSOR;ALUNO", "PROFESSOR,PROFESSOR;ALUNO", "ALUNO,ALUNO"})
    void papelDoTokenViraAHierarquiaCompletaNoHeaderParaOsServicosDeLeitura(String papel, String esperado) {
        MockServerWebExchange exchange = comCookie("/api/v1/livro/listar", keys.accessToken("2023001", papel));

        filter.filter(exchange, chain).block();

        assertThat(repassada.get().getRequest().getHeaders().getFirst("X-User-Roles")).isEqualTo(esperado.replace(";", ","));
    }

    @Test
    void ordemDoFiltroEAnteriorAoRoteamento() {
        assertThat(filter.getOrder()).isEqualTo(-1);
    }
}
