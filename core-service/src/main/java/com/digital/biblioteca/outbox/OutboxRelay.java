package com.digital.biblioteca.outbox;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * O unico lugar do core-service que efetivamente fala com o Kafka/RabbitMQ.
 * Roda a cada poucos segundos, pega um lote de linhas PENDING da outbox e
 * publica cada uma no broker certo. Se falhar, incrementa a tentativa e
 * deixa PENDING pra tentar de novo no proximo ciclo — so desiste (FAILED)
 * depois de varias tentativas seguidas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int TAMANHO_LOTE = 20;
    private static final int MAX_TENTATIVAS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publicarPendentes() {

        List<OutboxEvent> pendentes = outboxEventRepository.buscarPendentes(TAMANHO_LOTE);

        for (OutboxEvent evento : pendentes) {
            publicar(evento);
            outboxEventRepository.save(evento);
        }

    }

    private void publicar(OutboxEvent evento) {
        try {

            switch (evento.getBroker()) {
                case KAFKA -> publicarNoKafka(evento);
                case RABBITMQ -> rabbitTemplate.convertAndSend(evento.getDestination(), evento.getPayload(),
                        mensagem -> {
                            mensagem.getMessageProperties().setHeader(CorrelationId.HEADER, evento.getCorrelationId());
                            return mensagem;
                        });
            }

            evento.setStatus(OutboxStatus.SENT);
            evento.setDataEnvio(Instant.now());

        } catch (Exception e) {

            evento.setAttempts(evento.getAttempts() + 1);
            log.warn("Falha ao publicar evento de outbox {} (tentativa {}/{}): {}",
                    evento.getId(), evento.getAttempts(), MAX_TENTATIVAS, e.getMessage());

            if (evento.getAttempts() >= MAX_TENTATIVAS) {
                evento.setStatus(OutboxStatus.FAILED);
                log.error("Evento de outbox {} marcado como FAILED após {} tentativas — precisa de inspeção manual.",
                        evento.getId(), MAX_TENTATIVAS);
            }

        }
    }

    private void publicarNoKafka(OutboxEvent evento) throws Exception {

        DomainEvent envelope = new DomainEvent(
                UUID.randomUUID().toString(),
                evento.getAggregateType(),
                evento.getAggregateId(),
                evento.getEventType(),
                evento.getPayload(),
                evento.getDataCriacao(),
                evento.getCorrelationId()
        );

        String mensagem = objectMapper.writeValueAsString(envelope);

        // Particiona pelo aggregateId: eventos do mesmo agregado sempre
        // chegam em ordem pro consumidor, mesmo com varias particoes.
        kafkaTemplate.send(evento.getDestination(), evento.getAggregateId(), mensagem).get();

    }

}
