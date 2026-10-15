package com.digital.smartteca.storage.consumer;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.storage.service.SupabaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Base64;

import static com.digital.smartteca.storage.config.RabbitMQConfig.FILA_DELETE;
import static com.digital.smartteca.storage.config.RabbitMQConfig.FILA_UPLOAD;

/**
 * O container do @RabbitListener roda numa thread propria (nao no
 * event-loop reativo), entao dar .block() nos Mono do SupabaseService
 * aqui e seguro — nao e o mesmo cenario proibido de bloquear dentro de
 * uma requisicao WebFlux. Se a chamada ao Supabase falhar, a excecao
 * sobe, o listener rejeita a mensagem (defaultRequeueRejected=false) e
 * ela cai na fila morta em vez de se perder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCommandConsumer {

    private final SupabaseService supabaseService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = FILA_UPLOAD)
    public void consumirUpload(String mensagem,
                               @Header(value = CorrelationId.HEADER, required = false) String correlationId) throws Exception {

        try (var ignored = CorrelationId.open(correlationId)) {

            UploadArquivoCommand comando = objectMapper.readValue(mensagem, UploadArquivoCommand.class);

            byte[] conteudo = Base64.getDecoder().decode(comando.conteudoBase64());

            log.info("Processando upload: bucket={} nome={}", comando.bucket(), comando.nome());

            supabaseService.subir(conteudo, comando.contentType(), comando.bucket(), comando.nome()).block();

        }

    }

    @RabbitListener(queues = FILA_DELETE)
    public void consumirDelete(String mensagem,
                               @Header(value = CorrelationId.HEADER, required = false) String correlationId) throws Exception {

        try (var ignored = CorrelationId.open(correlationId)) {

            DeletarArquivoCommand comando = objectMapper.readValue(mensagem, DeletarArquivoCommand.class);

            log.info("Processando exclusão: bucket={} nome={}", comando.bucket(), comando.nome());

            supabaseService.deletar(comando.nome(), comando.bucket()).block();

        }

    }

}
