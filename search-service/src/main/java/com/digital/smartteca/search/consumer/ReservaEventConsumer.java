package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.repository.ReservaDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * "ReservaDevolvida" remove o documento do indice em vez de so atualizar
 * o status — comportamento herdado do ReservaEventListener original, nao
 * uma escolha nova. E uma limitacao conhecida (o historico de reservas
 * devolvidas some do Elasticsearch), documentada no plano da Fase 0.
 *
 * "ReservaAtrasada" (Fase 4) e o outro lado dessa mesma nota: no codigo
 * original o job que marcava ATRASADO so tocava o Postgres, nunca o ES —
 * o documento ficava com "RESERVADO" pra sempre. Agora o
 * NotificarAtraso.verificarAtraso() publica esse evento junto, e aqui a
 * gente so faz upsert com o novo status (mesmo payload de sempre, um
 * ReservaDocument).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaEventConsumer {

    private final ReservaDocumentRepository reservaDocumentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "reserva-events")
    public void consumir(String mensagem) throws Exception {

        DomainEvent evento = objectMapper.readValue(mensagem, DomainEvent.class);

        try (var ignored = CorrelationId.open(evento.correlationId())) {

            switch (evento.eventType()) {

                case "ReservaCriada", "ReservaAtrasada" -> {
                    ReservaDocument document = objectMapper.readValue(evento.payload(), ReservaDocument.class);
                    reservaDocumentRepository.save(document);
                }

                case "ReservaDevolvida" -> reservaDocumentRepository.deleteById(evento.aggregateId());

                default -> log.warn("Tipo de evento de Reserva desconhecido: {}", evento.eventType());

            }

        }

    }
}
