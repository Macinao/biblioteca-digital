package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.fanout.CursoFanOutService;
import com.digital.smartteca.search.repository.CursoDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consome o topico curso-events e mantem o indice "curso" do Elasticsearch
 * atualizado. Usar o aggregateId como id do documento (igual ao Postgres)
 * torna o save() idempotente — reprocessar a mesma mensagem duas vezes
 * (o outbox garante *at-least-once*, nunca exactly-once) so sobrescreve
 * com o mesmo valor, sem duplicar nada.
 *
 * Em CursoAtualizado, alem de atualizar o indice "curso" em si, propaga a
 * mudanca pras copias embutidas em livro/livro_publico/tcc/material_estudo
 * (ver CursoFanOutService). CursoExcluido nao precisa de fan-out: a copia
 * embutida so guarda o nome, que continua valido mesmo com o Curso
 * inativo no Postgres.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CursoEventConsumer {

    private final CursoDocumentRepository cursoDocumentRepository;
    private final CursoFanOutService cursoFanOutService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "curso-events")
    public void consumir(String mensagem) throws Exception {

        DomainEvent evento = objectMapper.readValue(mensagem, DomainEvent.class);

        try (var ignored = CorrelationId.open(evento.correlationId())) {

            switch (evento.eventType()) {

                case "CursoCadastrado" -> {
                    CursoDocument document = objectMapper.readValue(evento.payload(), CursoDocument.class);
                    cursoDocumentRepository.save(document);
                }

                case "CursoAtualizado" -> {
                    CursoDocument document = objectMapper.readValue(evento.payload(), CursoDocument.class);
                    cursoDocumentRepository.save(document);
                    cursoFanOutService.propagarAtualizacao(document);
                }

                case "CursoExcluido" -> cursoDocumentRepository.deleteById(evento.aggregateId());

                default -> log.warn("Tipo de evento de Curso desconhecido: {}", evento.eventType());

            }

        }

    }
}
