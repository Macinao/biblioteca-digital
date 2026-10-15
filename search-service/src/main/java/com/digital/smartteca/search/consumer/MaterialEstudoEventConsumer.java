package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialEstudoEventConsumer {

    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "material-estudo-events")
    public void consumir(String mensagem) throws Exception {

        DomainEvent evento = objectMapper.readValue(mensagem, DomainEvent.class);

        try (var ignored = CorrelationId.open(evento.correlationId())) {

            switch (evento.eventType()) {

                case "MaterialEstudoCadastrado", "MaterialEstudoAtualizado" -> {
                    MaterialEstudoDocument document = objectMapper.readValue(evento.payload(), MaterialEstudoDocument.class);
                    materialEstudoDocumentRepository.save(document);
                }

                case "MaterialEstudoExcluido" -> materialEstudoDocumentRepository.deleteById(evento.aggregateId());

                default -> log.warn("Tipo de evento de MaterialEstudo desconhecido: {}", evento.eventType());

            }

        }

    }
}
