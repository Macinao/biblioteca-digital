package com.digital.smartteca.storage;

import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.storage.config.RabbitMQConfig;
import com.digital.smartteca.storage.security.CorrelationIdWebFilter;
import com.digital.smartteca.storage.security.GatewayHeaderAuthWebFilter;
import com.digital.smartteca.storage.treatment.GlobalExceptionHandler;
import com.digital.smartteca.storage.treatment.IntegracaoExternaException;
import com.digital.smartteca.storage.treatment.RecursoNaoEncontradoException;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ConfigETratamentoTest {

    // ---------- RabbitMQConfig ----------

    final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void filasDeUploadEDeleteSaoDuraveisEApontamParaSuasFilasMortas() {
        Queue upload = config.uploadQueue();
        Queue delete = config.deleteQueue();

        assertThat(upload.getName()).isEqualTo("storage.upload");
        assertThat(upload.isDurable()).isTrue();
        assertThat(upload.getArguments()).containsEntry("x-dead-letter-exchange", "storage-dlx")
                .containsEntry("x-dead-letter-routing-key", "storage.upload.dlq");
        assertThat(delete.getName()).isEqualTo("storage.delete");
        assertThat(delete.getArguments()).containsEntry("x-dead-letter-routing-key", "storage.delete.dlq");
    }

    @Test
    void filasMortasEstaoLigadasAExchangeMortaComAsRoutingKeysCertas() {
        Binding upload = config.uploadDlqBinding();
        Binding delete = config.deleteDlqBinding();

        assertThat(upload.getExchange()).isEqualTo("storage-dlx");
        assertThat(upload.getRoutingKey()).isEqualTo("storage.upload.dlq");
        assertThat(upload.getDestination()).isEqualTo("storage.upload.dlq");
        assertThat(delete.getRoutingKey()).isEqualTo("storage.delete.dlq");
        assertThat(delete.getDestination()).isEqualTo("storage.delete.dlq");
        assertThat(config.uploadDlq().isDurable()).isTrue();
        assertThat(config.deleteDlq().isDurable()).isTrue();
    }

    @Test
    void mensagemRejeitadaNaoVoltaParaAFila() {
        SimpleRabbitListenerContainerFactory factory = config.rabbitListenerContainerFactory(mock(ConnectionFactory.class));

        assertThat(ReflectionTestUtils.getField(factory, "defaultRequeueRejected")).isEqualTo(false);
    }

    // ---------- GlobalExceptionHandler ----------

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/livro/baixar/capa/x"));
    }

    @Test
    void recursoNaoEncontradoVira404() {
        var resposta = handler.handleNotFound(new RecursoNaoEncontradoException("Arquivo não encontrado no Supabase"), exchange());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody().getMessage()).isEqualTo("Recurso não encontrado");
        assertThat(resposta.getBody().getDetails()).isEqualTo("Arquivo não encontrado no Supabase");
        assertThat(resposta.getBody().getPath()).isEqualTo("/api/v1/livro/baixar/capa/x");
        assertThat(resposta.getBody().getStatusCode()).isEqualTo(404);
    }

    @Test
    void integracaoExternaVira502() {
        var resposta = handler.handleIntegracaoExterna(new IntegracaoExternaException("Supabase fora"), exchange());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(resposta.getBody().getMessage()).isEqualTo("Falha em sistema externo");
    }

    @Test
    void acessoNegadoVira403EnaoMais500() {
        var resposta = handler.handleAccessDenied(new org.springframework.security.access.AccessDeniedException("Access Denied"), exchange());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody().getMessage()).isEqualTo("Acesso negado");
    }

    @Test
    void excecaoNaoMapeadaVira500() {
        var resposta = handler.handleGeneric(new IllegalStateException("boom"), exchange());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resposta.getBody().getStatus()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(resposta.getBody().getTimestamp()).matches("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}");
    }

    // ---------- GatewayHeaderAuthWebFilter ----------

    private Authentication autenticacaoDoContexto(MockServerWebExchange exchange) {
        AtomicReference<Authentication> capturada = new AtomicReference<>();
        WebFilterChain chain = ex -> ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .doOnNext(capturada::set)
                .then();
        new GatewayHeaderAuthWebFilter().filter(exchange, chain).block();
        return capturada.get();
    }

    @Test
    void headersDoGatewayViramAutenticacaoComPapeisEmROLEPrefixado() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("X-User-Matricula", "2023001").header("X-User-Roles", "PROFESSOR, ALUNO"));

        Authentication auth = autenticacaoDoContexto(exchange);

        assertThat(auth.getName()).isEqualTo("2023001");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactlyInAnyOrder("ROLE_PROFESSOR", "ROLE_ALUNO");
    }

    @Test
    void semHeadersNaoAutentica() {
        assertThat(autenticacaoDoContexto(MockServerWebExchange.from(MockServerHttpRequest.get("/x")))).isNull();
    }

    @Test
    void semMatriculaOuSemRolesNaoAutentica() {
        assertThat(autenticacaoDoContexto(MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("X-User-Roles", "ALUNO")))).isNull();
        assertThat(autenticacaoDoContexto(MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("X-User-Matricula", "2023001")))).isNull();
        assertThat(autenticacaoDoContexto(MockServerWebExchange.from(MockServerHttpRequest.get("/x")
                .header("X-User-Matricula", " ").header("X-User-Roles", "ALUNO")))).isNull();
    }

    // ---------- CorrelationIdWebFilter ----------

    @Test
    void correlationIdDoGatewayEEcoadoNaRespostaEGeradoQuandoAusente() {
        CorrelationIdWebFilter filter = new CorrelationIdWebFilter();
        MockServerWebExchange comId = MockServerWebExchange.from(MockServerHttpRequest.get("/x").header(CorrelationId.HEADER, "corr-9"));
        MockServerWebExchange semId = MockServerWebExchange.from(MockServerHttpRequest.get("/x"));

        filter.filter(comId, ex -> Mono.empty()).block();
        filter.filter(semId, ex -> Mono.empty()).block();

        assertThat(comId.getResponse().getHeaders().getFirst(CorrelationId.HEADER)).isEqualTo("corr-9");
        assertThat(semId.getResponse().getHeaders().getFirst(CorrelationId.HEADER)).isNotBlank();
    }
}
