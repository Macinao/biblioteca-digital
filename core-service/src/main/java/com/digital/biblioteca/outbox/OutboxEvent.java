package com.digital.biblioteca.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha aqui e gravada na MESMA transacao que o dado de negocio (ex.:
 * salvar um Curso). E o que garante que o evento nunca se perde entre o
 * commit no Postgres e a publicacao de fato no broker — o OutboxRelay
 * cuida da publicacao, de forma assincrona, num processo separado.
 */
@Entity
@Table(name = "outbox_event")
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Broker broker;

    @Column(nullable = false)
    private String destination;

    // Capturado do MDC no momento do record() — o OutboxRelay so publica
    // segundos depois, entao precisa ficar guardado ate la. Nullable
    // porque nem todo evento nasce de uma requisicao HTTP.
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant dataCriacao;

    private Instant dataEnvio;

}
