package com.digital.smartteca.common.commands;

import com.digital.smartteca.common.enums.BucketSupabase;

/**
 * Comando publicado no RabbitMQ (via outbox, broker=RABBITMQ) pedindo pro
 * storage-service subir um arquivo no Supabase. O conteudo vai em base64
 * porque a mensagem e so texto/JSON — nao da pra mandar um MultipartFile
 * pela fila. Para portfolio o tamanho maximo de upload ja e limitado a
 * 10MB (spring.servlet.multipart no core-service), entao base64 aqui e
 * uma escolha aceitavel; um arquivo bem maior pediria upload direto pra
 * um storage intermediario em vez de passar pela fila.
 */
public record UploadArquivoCommand(
        BucketSupabase bucket,
        String nome,
        String contentType,
        String conteudoBase64
) {
}
