package com.digital.smartteca.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void filaPrincipalEDuravelEApontaParaADeadLetterExchange() {
        Queue fila = config.whatsappQueue();

        assertThat(fila.getName()).isEqualTo("notification.whatsapp.send");
        assertThat(fila.isDurable()).isTrue();
        assertThat(fila.getArguments()).containsEntry("x-dead-letter-exchange", "notification-dlx")
                .containsEntry("x-dead-letter-routing-key", "notification.whatsapp.send.dlq");
    }

    @Test
    void filaMortaEDuravelELigadaAExchangeMortaPelaRoutingKeyCerta() {
        Queue dlq = config.whatsappDlq();
        Binding binding = config.whatsappDlqBinding();

        assertThat(dlq.getName()).isEqualTo("notification.whatsapp.send.dlq");
        assertThat(dlq.isDurable()).isTrue();
        assertThat(binding.getExchange()).isEqualTo("notification-dlx");
        assertThat(binding.getRoutingKey()).isEqualTo("notification.whatsapp.send.dlq");
        assertThat(binding.getDestination()).isEqualTo("notification.whatsapp.send.dlq");
    }

    @Test
    void mensagemRejeitadaNaoVoltaParaAFilaPrincipalSenaoViraLoopInfinito() {
        SimpleRabbitListenerContainerFactory factory = config.rabbitListenerContainerFactory(mock(ConnectionFactory.class));

        assertThat(ReflectionTestUtils.getField(factory, "defaultRequeueRejected")).isEqualTo(false);
    }
}
