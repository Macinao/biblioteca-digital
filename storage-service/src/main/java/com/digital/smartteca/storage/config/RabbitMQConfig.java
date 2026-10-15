package com.digital.smartteca.storage.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * As filas storage.upload/storage.delete recebem mensagem pela exchange
 * padrao do RabbitMQ (o OutboxRelay so faz convertAndSend(destination,
 * payload) — routing key = nome da fila, sem exchange nomeada — e o
 * RabbitMQ liga toda fila a exchange padrao usando o proprio nome como
 * chave, entao isso funciona sem precisar declarar binding pra elas).
 *
 * O que precisamos declarar de verdade e a fila morta: se o consumer
 * falhar ao processar (Supabase fora do ar, bucket invalido, etc.), a
 * mensagem NAO pode simplesmente sumir — cai na storage-dlx em vez de
 * ser perdida, ficando disponivel pra inspecao manual.
 */
@Configuration
public class RabbitMQConfig {

    public static final String FILA_UPLOAD = "storage.upload";
    public static final String FILA_DELETE = "storage.delete";
    private static final String DLX = "storage-dlx";
    private static final String FILA_UPLOAD_DLQ = "storage.upload.dlq";
    private static final String FILA_DELETE_DLQ = "storage.delete.dlq";

    @Bean
    public DirectExchange storageDlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue uploadQueue() {
        return QueueBuilder.durable(FILA_UPLOAD)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", FILA_UPLOAD_DLQ)
                .build();
    }

    @Bean
    public Queue deleteQueue() {
        return QueueBuilder.durable(FILA_DELETE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", FILA_DELETE_DLQ)
                .build();
    }

    @Bean
    public Queue uploadDlq() {
        return QueueBuilder.durable(FILA_UPLOAD_DLQ).build();
    }

    @Bean
    public Queue deleteDlq() {
        return QueueBuilder.durable(FILA_DELETE_DLQ).build();
    }

    @Bean
    public Binding uploadDlqBinding() {
        return BindingBuilder.bind(uploadDlq()).to(storageDlx()).with(FILA_UPLOAD_DLQ);
    }

    @Bean
    public Binding deleteDlqBinding() {
        return BindingBuilder.bind(deleteDlq()).to(storageDlx()).with(FILA_DELETE_DLQ);
    }

    /**
     * defaultRequeueRejected(false): se o listener lancar excecao, a
     * mensagem e rejeitada sem voltar pra fila original — vai direto pra
     * DLQ (via x-dead-letter-exchange da fila) em vez de ficar reprocessando
     * em loop infinito.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;

    }

}
