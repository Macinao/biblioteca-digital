package com.digital.biblioteca.outbox;

import com.digital.smartteca.common.observability.CorrelationId;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Ponto unico que os *ServiceImpl chamam para registrar um evento. So faz
 * um insert local (sem I/O de rede) — por isso pode rodar dentro da mesma
 * transacao do save() de negocio sem colocar a latencia do Kafka/RabbitMQ
 * no meio da resposta ao usuario.
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void record(String aggregateType, String aggregateId, String eventType,
                        Object payload, Broker broker, String destination) {

        OutboxEvent evento = new OutboxEvent();
        evento.setAggregateType(aggregateType);
        evento.setAggregateId(aggregateId);
        evento.setEventType(eventType);
        evento.setPayload(serializar(payload));
        evento.setBroker(broker);
        evento.setDestination(destination);
        evento.setStatus(OutboxStatus.PENDING);
        evento.setAttempts(0);
        evento.setDataCriacao(Instant.now());
        evento.setCorrelationId(CorrelationId.resolve(MDC.get(CorrelationId.MDC_KEY)));

        outboxEventRepository.save(evento);

    }

    private String serializar(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar payload do evento de outbox", e);
        }
    }

}
