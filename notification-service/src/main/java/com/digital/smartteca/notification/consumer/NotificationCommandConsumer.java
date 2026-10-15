package com.digital.smartteca.notification.consumer;

import com.digital.smartteca.common.commands.EnviarWhatsappCommand;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.notification.service.MetodosMensagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import static com.digital.smartteca.notification.config.RabbitMQConfig.FILA_WHATSAPP;

/**
 * Cada mensagem de WhatsApp e um item de fila independente — diferente
 * do job antigo (NotificarAtraso.notificarAtraso), que percorria todas as
 * reservas atrasadas num laço so e uma falha no meio abortava o resto.
 * Aqui, se o envio de uma reserva falhar, so essa mensagem cai na fila
 * morta; as outras continuam sendo processadas normalmente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCommandConsumer {

    private final MetodosMensagem metodosMensagem;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = FILA_WHATSAPP)
    public void consumirEnvioWhatsapp(String mensagem,
                                      @Header(value = CorrelationId.HEADER, required = false) String correlationId) throws Exception {

        try (var ignored = CorrelationId.open(correlationId)) {

            EnviarWhatsappCommand comando = objectMapper.readValue(mensagem, EnviarWhatsappCommand.class);

            log.info("Enviando WhatsApp para {}", comando.numero());

            metodosMensagem.enviarMensagem(comando.numero(), comando.mensagem()).block();

        }

    }

}
