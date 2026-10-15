package com.digital.biblioteca.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declara os topicos que o core-service publica — um por agregado, seguindo
 * o plano da Fase 1. Reserva usa seu proprio topico, mas tambem reaproveita
 * livro-events (a mudanca de estoque do Livro é publicada como um evento de
 * Livro de verdade, nao um evento de Reserva).
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic cursoEventsTopic() {
        return TopicBuilder.name("curso-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic temaEventsTopic() {
        return TopicBuilder.name("tema-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic livroEventsTopic() {
        return TopicBuilder.name("livro-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic livroPublicoEventsTopic() {
        return TopicBuilder.name("livro-publico-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic tccEventsTopic() {
        return TopicBuilder.name("tcc-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic materialEstudoEventsTopic() {
        return TopicBuilder.name("material-estudo-events").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic reservaEventsTopic() {
        return TopicBuilder.name("reserva-events").partitions(3).replicas(1).build();
    }

}
