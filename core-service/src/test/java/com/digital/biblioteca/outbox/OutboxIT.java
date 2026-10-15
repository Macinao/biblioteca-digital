package com.digital.biblioteca.outbox;

import com.digital.biblioteca.IntegracaoBase;
import com.digital.smartteca.common.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Outbox contra infraestrutura real: Postgres (FOR UPDATE SKIP LOCKED) e Kafka + RabbitMQ de verdade.
 * O OutboxRelay "de producao" vem mockado na base; aqui montamos um relay novo apontando pros containers.
 */
class OutboxIT extends IntegracaoBase {

    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.0");
    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");

    static {
        KAFKA.start();
        RABBIT.start();
    }

    @Autowired OutboxEventRepository repository;
    @Autowired ObjectMapper objectMapper;

    OutboxRelay relay;
    RabbitTemplate rabbitTemplate;
    KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void montarRelay() {
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", KAFKA.getBootstrapServers());
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
        connectionFactory.setUsername(RABBIT.getAdminUsername());
        connectionFactory.setPassword(RABBIT.getAdminPassword());
        rabbitTemplate = new RabbitTemplate(connectionFactory);

        relay = new OutboxRelay(repository, kafkaTemplate, rabbitTemplate, objectMapper);
    }

    private OutboxEvent pendente(Broker broker, String destino, String tipo, Instant criadoEm) {
        OutboxEvent evento = new OutboxEvent();
        evento.setAggregateType("Livro");
        evento.setAggregateId("livro-" + UUID.randomUUID());
        evento.setEventType(tipo);
        evento.setPayload("{\"titulo\":\"Clean Code\"}");
        evento.setBroker(broker);
        evento.setDestination(destino);
        evento.setCorrelationId("corr-it");
        evento.setStatus(OutboxStatus.PENDING);
        evento.setDataCriacao(criadoEm);
        return repository.save(evento);
    }

    private List<ConsumerRecord<String, String>> consumirKafka(String topico, int esperados) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        List<ConsumerRecord<String, String>> lidos = new java.util.ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topico));
            long limite = System.currentTimeMillis() + 20_000;
            while (lidos.size() < esperados && System.currentTimeMillis() < limite) {
                consumer.poll(Duration.ofMillis(500)).forEach(lidos::add);
            }
        }
        return lidos;
    }

    // ---------- kafka ----------

    @Test
    void eventoPendenteDeKafkaChegaNoTopicoComChaveEEnvelopeEFicaSent() throws Exception {
        OutboxEvent evento = pendente(Broker.KAFKA, "livro-events", "LivroCadastrado", Instant.now());

        relay.publicarPendentes();

        List<ConsumerRecord<String, String>> lidos = consumirKafka("livro-events", 1);
        assertThat(lidos).hasSize(1);
        assertThat(lidos.get(0).key()).isEqualTo(evento.getAggregateId());
        DomainEvent envelope = objectMapper.readValue(lidos.get(0).value(), DomainEvent.class);
        assertThat(envelope.eventType()).isEqualTo("LivroCadastrado");
        assertThat(envelope.aggregateId()).isEqualTo(evento.getAggregateId());
        assertThat(envelope.payload()).isEqualTo("{\"titulo\":\"Clean Code\"}");
        assertThat(envelope.correlationId()).isEqualTo("corr-it");

        OutboxEvent depois = repository.findById(evento.getId()).orElseThrow();
        assertThat(depois.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(depois.getDataEnvio()).isNotNull();
    }

    @Test
    void eventosDoMesmoAgregadoChegamEmOrdemNaMesmaParticao() throws Exception {
        String agregado = "livro-ordem";
        Instant base = Instant.now();
        for (int i = 0; i < 5; i++) {
            OutboxEvent evento = pendente(Broker.KAFKA, "reserva-events", "Evento" + i, base.plusMillis(i));
            evento.setAggregateId(agregado);
            repository.save(evento);
        }

        relay.publicarPendentes();

        List<ConsumerRecord<String, String>> lidos = consumirKafka("reserva-events", 5);
        assertThat(lidos).hasSize(5);
        assertThat(lidos).extracting(ConsumerRecord::partition).containsOnly(lidos.get(0).partition());
        assertThat(lidos.stream().map(r -> {
            try {
                return objectMapper.readValue(r.value(), DomainEvent.class).eventType();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        })).containsExactly("Evento0", "Evento1", "Evento2", "Evento3", "Evento4");
    }

    // ---------- rabbit ----------

    @Test
    void comandoPendenteDeRabbitChegaNaFilaComCorrelationIdNoHeaderEFicaSent() {
        String fila = "storage.upload." + UUID.randomUUID();
        new RabbitAdmin(rabbitTemplate.getConnectionFactory()).declareQueue(new Queue(fila));
        OutboxEvent evento = pendente(Broker.RABBITMQ, fila, "ArquivoParaSubir", Instant.now());

        relay.publicarPendentes();

        Message mensagem = rabbitTemplate.receive(fila, 10_000);
        assertThat(mensagem).isNotNull();
        assertThat(new String(mensagem.getBody())).contains("Clean Code");
        assertThat((String) mensagem.getMessageProperties().getHeader("X-Correlation-Id")).isEqualTo("corr-it");
        assertThat(repository.findById(evento.getId()).orElseThrow().getStatus()).isEqualTo(OutboxStatus.SENT);
    }

    // ---------- falha e retentativa ----------

    @Test
    void comKafkaIndisponivelOEventoContinuaPendenteIncrementaTentativasEViraFailedNaQuinta() {
        OutboxEvent evento = pendente(Broker.KAFKA, "livro-events", "LivroCadastrado", Instant.now());
        Map<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", "localhost:1");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("max.block.ms", "500");
        props.put("delivery.timeout.ms", "1000");
        props.put("request.timeout.ms", "500");
        OutboxRelay relayComBrokerFora = new OutboxRelay(repository,
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props)), rabbitTemplate, objectMapper);

        relayComBrokerFora.publicarPendentes();
        OutboxEvent aposPrimeira = repository.findById(evento.getId()).orElseThrow();
        assertThat(aposPrimeira.getAttempts()).isEqualTo(1);
        assertThat(aposPrimeira.getStatus()).isEqualTo(OutboxStatus.PENDING);

        for (int i = 0; i < 4; i++) {
            relayComBrokerFora.publicarPendentes();
        }
        OutboxEvent aposQuinta = repository.findById(evento.getId()).orElseThrow();
        assertThat(aposQuinta.getAttempts()).isEqualTo(5);
        assertThat(aposQuinta.getStatus()).isEqualTo(OutboxStatus.FAILED);

        relayComBrokerFora.publicarPendentes();
        assertThat(repository.findById(evento.getId()).orElseThrow().getAttempts()).isEqualTo(5);
    }

    // ---------- repositorio do outbox ----------

    @Test
    void buscarPendentesTrazSoPendentesDoMaisAntigoParaOMaisNovoRespeitandoOLimite() {
        Instant base = Instant.now();
        OutboxEvent maisNovo = pendente(Broker.KAFKA, "t", "C", base.plusSeconds(3));
        OutboxEvent maisAntigo = pendente(Broker.KAFKA, "t", "A", base.plusSeconds(1));
        OutboxEvent meio = pendente(Broker.KAFKA, "t", "B", base.plusSeconds(2));
        OutboxEvent enviado = pendente(Broker.KAFKA, "t", "ja-enviado", base);
        enviado.setStatus(OutboxStatus.SENT);
        repository.save(enviado);
        OutboxEvent falho = pendente(Broker.KAFKA, "t", "falho", base);
        falho.setStatus(OutboxStatus.FAILED);
        repository.save(falho);

        List<OutboxEvent> todos = new TransactionTemplate(transactionManager).execute(s -> repository.buscarPendentes(10));
        List<OutboxEvent> dois = new TransactionTemplate(transactionManager).execute(s -> repository.buscarPendentes(2));

        assertThat(todos).extracting(OutboxEvent::getId).containsExactly(maisAntigo.getId(), meio.getId(), maisNovo.getId());
        assertThat(dois).extracting(OutboxEvent::getId).containsExactly(maisAntigo.getId(), meio.getId());
    }

    @Test
    void duasInstanciasDoRelayNaoPegamAsMesmasLinhasGracasAoSkipLocked() throws Exception {
        Instant base = Instant.now();
        OutboxEvent a = pendente(Broker.KAFKA, "t", "A", base.plusSeconds(1));
        OutboxEvent b = pendente(Broker.KAFKA, "t", "B", base.plusSeconds(2));
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        CountDownLatch primeiraPegou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        AtomicReference<List<OutboxEvent>> daPrimeira = new AtomicReference<>();

        Thread primeira = new Thread(() -> tx.executeWithoutResult(s -> {
            daPrimeira.set(repository.buscarPendentes(1));
            primeiraPegou.countDown();
            try {
                liberar.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        primeira.start();
        assertThat(primeiraPegou.await(15, TimeUnit.SECONDS)).isTrue();

        List<OutboxEvent> daSegunda = tx.execute(s -> repository.buscarPendentes(10));
        liberar.countDown();
        primeira.join(15_000);

        assertThat(daPrimeira.get()).extracting(OutboxEvent::getId).containsExactly(a.getId());
        assertThat(daSegunda).extracting(OutboxEvent::getId).containsExactly(b.getId());
    }
}
