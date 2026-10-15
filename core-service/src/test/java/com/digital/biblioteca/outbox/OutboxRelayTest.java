package com.digital.biblioteca.outbox;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class OutboxRelayTest {

    final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    OutboxRelay relay;

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, kafka, rabbit, objectMapper);
    }

    private static OutboxEvent kafkaEvent() {
        OutboxEvent evento = new OutboxEvent();
        evento.setId(UUID.randomUUID());
        evento.setAggregateType("Livro");
        evento.setAggregateId("livro-42");
        evento.setEventType("LivroCadastrado");
        evento.setPayload("{\"titulo\":\"Clean Code\"}");
        evento.setBroker(Broker.KAFKA);
        evento.setDestination("livro-events");
        evento.setCorrelationId("corr-1");
        evento.setStatus(OutboxStatus.PENDING);
        evento.setDataCriacao(Instant.parse("2026-02-01T12:00:00Z"));
        return evento;
    }

    private static OutboxEvent rabbitEvent() {
        OutboxEvent evento = kafkaEvent();
        evento.setBroker(Broker.RABBITMQ);
        evento.setDestination("storage.upload");
        evento.setEventType("ArquivoParaSubir");
        return evento;
    }

    private void kafkaResponde() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void semPendentesNaoPublicaNemGrava() {
        when(repository.buscarPendentes(20)).thenReturn(List.of());

        relay.publicarPendentes();

        verify(kafka, never()).send(anyString(), anyString(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void buscaLotesDeVinteEventos() {
        when(repository.buscarPendentes(20)).thenReturn(List.of());

        relay.publicarPendentes();

        verify(repository).buscarPendentes(20);
    }

    @Test
    void eventoKafkaVaiNoEnvelopeDomainEventParticionadoPeloAggregateId() throws Exception {
        OutboxEvent evento = kafkaEvent();
        when(repository.buscarPendentes(20)).thenReturn(List.of(evento));
        kafkaResponde();
        Instant antes = Instant.now();

        relay.publicarPendentes();

        ArgumentCaptor<String> mensagem = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq("livro-events"), eq("livro-42"), mensagem.capture());
        DomainEvent envelope = objectMapper.readValue(mensagem.getValue(), DomainEvent.class);
        assertThat(UUID.fromString(envelope.eventId())).isNotNull();
        assertThat(envelope.aggregateType()).isEqualTo("Livro");
        assertThat(envelope.aggregateId()).isEqualTo("livro-42");
        assertThat(envelope.eventType()).isEqualTo("LivroCadastrado");
        assertThat(envelope.payload()).isEqualTo("{\"titulo\":\"Clean Code\"}");
        assertThat(envelope.occurredAt()).isEqualTo(Instant.parse("2026-02-01T12:00:00Z"));
        assertThat(envelope.correlationId()).isEqualTo("corr-1");

        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(evento.getDataEnvio()).isBetween(antes, Instant.now());
        verify(repository).save(evento);
    }

    @Test
    void cadaEntregaKafkaGeraUmEventIdNovo() throws Exception {
        OutboxEvent a = kafkaEvent();
        OutboxEvent b = kafkaEvent();
        when(repository.buscarPendentes(20)).thenReturn(List.of(a, b));
        kafkaResponde();

        relay.publicarPendentes();

        ArgumentCaptor<String> mensagens = ArgumentCaptor.forClass(String.class);
        verify(kafka, org.mockito.Mockito.times(2)).send(anyString(), anyString(), mensagens.capture());
        String idA = objectMapper.readValue(mensagens.getAllValues().get(0), DomainEvent.class).eventId();
        String idB = objectMapper.readValue(mensagens.getAllValues().get(1), DomainEvent.class).eventId();
        assertThat(idA).isNotEqualTo(idB);
    }

    @Test
    void eventoRabbitVaiParaAFilaComOCorrelationIdNoHeader() {
        OutboxEvent evento = rabbitEvent();
        when(repository.buscarPendentes(20)).thenReturn(List.of(evento));

        relay.publicarPendentes();

        ArgumentCaptor<MessagePostProcessor> processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbit).convertAndSend(eq("storage.upload"), eq((Object) "{\"titulo\":\"Clean Code\"}"), processor.capture());
        Message mensagem = new Message("x".getBytes(), new MessageProperties());
        processor.getValue().postProcessMessage(mensagem);
        assertThat((String) mensagem.getMessageProperties().getHeader(CorrelationId.HEADER)).isEqualTo("corr-1");

        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(evento.getDataEnvio()).isNotNull();
        verify(repository).save(evento);
        verify(kafka, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void falhaNoKafkaIncrementaATentativaEMantemPendente() {
        OutboxEvent evento = kafkaEvent();
        when(repository.buscarPendentes(20)).thenReturn(List.of(evento));
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker fora do ar")));

        relay.publicarPendentes();

        assertThat(evento.getAttempts()).isEqualTo(1);
        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(evento.getDataEnvio()).isNull();
        verify(repository).save(evento);
    }

    @Test
    void falhaNoRabbitIncrementaATentativaEMantemPendente() {
        OutboxEvent evento = rabbitEvent();
        when(repository.buscarPendentes(20)).thenReturn(List.of(evento));
        doThrow(new AmqpConnectException(new RuntimeException("sem conexao")))
                .when(rabbit).convertAndSend(anyString(), any(Object.class), any(MessagePostProcessor.class));

        relay.publicarPendentes();

        assertThat(evento.getAttempts()).isEqualTo(1);
        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void naoMarcaFailedAnteDaQuintaTentativaSeguida() {
        OutboxEvent evento = kafkaEvent();
        evento.setAttempts(3);
        when(repository.buscarPendentes(20)).thenReturn(List.of(evento));
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("falha")));

        relay.publicarPendentes();

        assertThat(evento.getAttempts()).isEqualTo(4);
        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void naQuintaFalhaSeguidaMarcaFailedParaInspecaoManual() {
        OutboxEvent evento = kafkaEvent();
        evento.setAttempts(4);
        when(repository.buscarPendentes(20)).thenReturn(List.of(evento));
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("falha")));

        relay.publicarPendentes();

        assertThat(evento.getAttempts()).isEqualTo(5);
        assertThat(evento.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(repository).save(evento);
    }

    @Test
    void umEventoComFalhaNaoImpedeOsDemaisDoLote() {
        OutboxEvent falha = kafkaEvent();
        falha.setAggregateId("com-falha");
        OutboxEvent ok = kafkaEvent();
        ok.setAggregateId("ok");
        when(repository.buscarPendentes(20)).thenReturn(List.of(falha, ok));
        when(kafka.send(anyString(), eq("com-falha"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("falha")));
        when(kafka.send(anyString(), eq("ok"), anyString())).thenReturn(CompletableFuture.completedFuture(null));

        relay.publicarPendentes();

        assertThat(falha.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(ok.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(repository).save(falha);
        verify(repository).save(ok);
    }
}
