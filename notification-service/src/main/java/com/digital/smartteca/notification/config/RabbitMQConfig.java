package com.digital.smartteca.notification.config;

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
 * Mesmo desenho do storage-service: fila principal recebendo pela
 * exchange padrao (routing key = nome da fila), com dead-letter-exchange
 * propria pra falha de envio nao desaparecer.
 */
@Configuration
public class RabbitMQConfig {

    public static final String FILA_WHATSAPP = "notification.whatsapp.send";
    private static final String DLX = "notification-dlx";
    private static final String FILA_WHATSAPP_DLQ = "notification.whatsapp.send.dlq";

    @Bean
    public DirectExchange notificationDlx() {
        return new DirectExchange(DLX);
    }

    @Bean
    public Queue whatsappQueue() {
        return QueueBuilder.durable(FILA_WHATSAPP)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", FILA_WHATSAPP_DLQ)
                .build();
    }

    @Bean
    public Queue whatsappDlq() {
        return QueueBuilder.durable(FILA_WHATSAPP_DLQ).build();
    }

    @Bean
    public Binding whatsappDlqBinding() {
        return BindingBuilder.bind(whatsappDlq()).to(notificationDlx()).with(FILA_WHATSAPP_DLQ);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setDefaultRequeueRejected(false);
        return factory;

    }

}
