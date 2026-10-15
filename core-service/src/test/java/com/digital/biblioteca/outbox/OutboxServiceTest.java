package com.digital.biblioteca.outbox;

import com.digital.biblioteca.document.CursoDocument;
import com.digital.smartteca.common.observability.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class OutboxServiceTest {

    final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    final OutboxService service = new OutboxService(repository, objectMapper);

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    private OutboxEvent gravado() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void gravaUmaLinhaPendenteComTodosOsCamposDoEvento() {
        CursoDocument payload = new CursoDocument();
        payload.setId("c-1");
        payload.setNome("Direito");
        Instant antes = Instant.now();

        service.record("Curso", "c-1", "CursoCadastrado", payload, Broker.KAFKA, "curso-events");

        OutboxEvent evento = gravado();
        assertThat(evento.getAggregateType()).isEqualTo("Curso");
        assertThat(evento.getAggregateId()).isEqualTo("c-1");
        assertThat(evento.getEventType()).isEqualTo("CursoCadastrado");
        assertThat(evento.getBroker()).isEqualTo(Broker.KAFKA);
        assertThat(evento.getDestination()).isEqualTo("curso-events");
        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(evento.getAttempts()).isZero();
        assertThat(evento.getDataEnvio()).isNull();
        assertThat(evento.getDataCriacao()).isBetween(antes, Instant.now());
    }

    @Test
    void serializaOPayloadComoJson() throws Exception {
        CursoDocument payload = new CursoDocument();
        payload.setId("c-1");
        payload.setNome("Direito");

        service.record("Curso", "c-1", "CursoCadastrado", payload, Broker.KAFKA, "curso-events");

        var json = objectMapper.readTree(gravado().getPayload());
        assertThat(json.get("id").asText()).isEqualTo("c-1");
        assertThat(json.get("nome").asText()).isEqualTo("Direito");
    }

    @Test
    void guardaOCorrelationIdQueEstaNoMdcNoMomentoDoRecord() {
        MDC.put(CorrelationId.MDC_KEY, "corr-da-requisicao");

        service.record("Curso", "c-1", "CursoCadastrado", new CursoDocument(), Broker.KAFKA, "curso-events");

        assertThat(gravado().getCorrelationId()).isEqualTo("corr-da-requisicao");
    }

    @Test
    void geraUmCorrelationIdQuandoNaoHaRequisicaoDeOrigem() {
        service.record("Reserva", "r-1", "ReservaAtrasada", new CursoDocument(), Broker.KAFKA, "reserva-events");

        assertThat(UUID.fromString(gravado().getCorrelationId())).isNotNull();
    }

    @Test
    void aceitaBrokerRabbitMqParaComandos() {
        service.record("Livro", "l-1", "ArquivoParaSubir", new CursoDocument(), Broker.RABBITMQ, "storage.upload");

        OutboxEvent evento = gravado();
        assertThat(evento.getBroker()).isEqualTo(Broker.RABBITMQ);
        assertThat(evento.getDestination()).isEqualTo("storage.upload");
    }

    @Test
    void payloadQueNaoSerializaFalhaComIllegalStateENaoGravaNada() {
        Object naoSerializavel = new Object();

        assertThatThrownBy(() -> service.record("X", "1", "Evento", naoSerializavel, Broker.KAFKA, "topico"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Falha ao serializar payload do evento de outbox");

        verifyNoInteractions(repository);
    }
}
