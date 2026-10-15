package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * "LivroAtualizado" chega tanto do proprio LivroServiceImpl.atualizar()
 * quanto do ReservaServiceImpl (reservar/devolver, quando o estoque
 * muda) — os dois publicam no mesmo topico com o mesmo formato de
 * payload, entao o consumer nem precisa saber a origem.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LivroEventConsumer {

    private final LivroDocumentRepository livroDocumentRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "livro-events")
    public void consumir(String mensagem) throws Exception {

        DomainEvent evento = objectMapper.readValue(mensagem, DomainEvent.class);

        try (var ignored = CorrelationId.open(evento.correlationId())) {

            switch (evento.eventType()) {

                case "LivroCadastrado", "LivroAtualizado" -> {
                    LivroDocument document = objectMapper.readValue(evento.payload(), LivroDocument.class);
                    livroDocumentRepository.save(document);
                }

                case "LivroExcluido" -> livroDocumentRepository.deleteById(evento.aggregateId());

                default -> log.warn("Tipo de evento de Livro desconhecido: {}", evento.eventType());

            }

        }

    }
}
