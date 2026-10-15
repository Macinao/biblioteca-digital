package com.digital.smartteca.common.events;

import java.time.Instant;

/**
 * Envelope publicado no Kafka para todo evento de dominio. O "payload" ja
 * vem serializado em JSON (o formato do documento de leitura do agregado,
 * ex.: CursoDocument) — quem consome (o search-service, na Fase 2) sabe
 * como desserializar de acordo com o eventType.
 */
public record DomainEvent(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        Instant occurredAt,
        String correlationId
) {
}
