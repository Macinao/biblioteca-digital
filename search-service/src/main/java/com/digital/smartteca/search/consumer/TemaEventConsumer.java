package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.TemaDocument;
import com.digital.smartteca.search.fanout.TemaFanOutService;
import com.digital.smartteca.search.repository.TemaDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TemaEventConsumer {

    private final TemaDocumentRepository temaDocumentRepository;
    private final TemaFanOutService temaFanOutService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "tema-events")
    public void consumir(String mensagem) throws Exception {

        DomainEvent evento = objectMapper.readValue(mensagem, DomainEvent.class);

        try (var ignored = CorrelationId.open(evento.correlationId())) {

            switch (evento.eventType()) {

                case "TemaCadastrado" -> {
                    TemaDocument document = objectMapper.readValue(evento.payload(), TemaDocument.class);
                    temaDocumentRepository.save(document);
                }

                case "TemaAtualizado" -> {
                    TemaDocument document = objectMapper.readValue(evento.payload(), TemaDocument.class);
                    temaDocumentRepository.save(document);
                    temaFanOutService.propagarAtualizacao(document);
                }

                case "TemaExcluido" -> temaDocumentRepository.deleteById(evento.aggregateId());

                default -> log.warn("Tipo de evento de Tema desconhecido: {}", evento.eventType());

            }

        }

    }
}
