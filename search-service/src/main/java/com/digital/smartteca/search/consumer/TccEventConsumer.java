package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TccEventConsumer {

    private final TccDocumentRepository tccDocumentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "tcc-events")
    public void consumir(String mensagem) throws Exception {

        DomainEvent evento = objectMapper.readValue(mensagem, DomainEvent.class);

        try (var ignored = CorrelationId.open(evento.correlationId())) {

            switch (evento.eventType()) {

                case "TccCadastrado", "TccAtualizado" -> {
                    TccDocument document = objectMapper.readValue(evento.payload(), TccDocument.class);
                    tccDocumentRepository.save(document);
                }

                case "TccExcluido" -> tccDocumentRepository.deleteById(evento.aggregateId());

                default -> log.warn("Tipo de evento de Tcc desconhecido: {}", evento.eventType());

            }

        }

    }
}
