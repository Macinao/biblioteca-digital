package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LivroPublicoEventConsumer {

    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "livro-publico-events")
    public void consumir(String mensagem) throws Exception {

        DomainEvent evento = objectMapper.readValue(mensagem, DomainEvent.class);

        try (var ignored = CorrelationId.open(evento.correlationId())) {

            switch (evento.eventType()) {

                case "LivroPublicoCadastrado", "LivroPublicoAtualizado" -> {
                    LivroPublicoDocument document = objectMapper.readValue(evento.payload(), LivroPublicoDocument.class);
                    livroPublicoDocumentRepository.save(document);
                }

                case "LivroPublicoExcluido" -> livroPublicoDocumentRepository.deleteById(evento.aggregateId());

                default -> log.warn("Tipo de evento de LivroPublico desconhecido: {}", evento.eventType());

            }

        }

    }
}
