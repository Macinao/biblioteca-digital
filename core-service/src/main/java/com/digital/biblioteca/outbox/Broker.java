package com.digital.biblioteca.outbox;

/**
 * Para qual broker uma linha da outbox deve ser publicada. KAFKA carrega
 * fatos de dominio (varios consumidores independentes); RABBITMQ carrega
 * comandos (uma tarefa especifica, com retry/DLQ).
 */
public enum Broker {
    KAFKA,
    RABBITMQ
}
