package com.digital.smartteca.common.commands;

/**
 * Comando publicado no RabbitMQ (via outbox, broker=RABBITMQ) pedindo pro
 * notification-service mandar uma mensagem de WhatsApp. O texto ja vem
 * pronto do core-service — o notification-service so entrega, nao decide
 * o que escrever (mesma separacao de responsabilidade do
 * UploadArquivoCommand: quem publica sabe o "o que", quem consome so faz
 * o "como entregar").
 */
public record EnviarWhatsappCommand(
        String numero,
        String mensagem
) {
}
