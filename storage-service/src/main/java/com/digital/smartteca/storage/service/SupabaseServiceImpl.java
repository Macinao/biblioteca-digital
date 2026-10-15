package com.digital.smartteca.storage.service;

import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.smartteca.storage.config.SupabaseProperties;
import com.digital.smartteca.storage.treatment.IntegracaoExternaException;
import com.digital.smartteca.storage.treatment.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reescrito com WebClient em vez do RestTemplate+byte[] original do
 * core-service — o download agora e transmitido em stream
 * (Flux<DataBuffer>) direto do Supabase pro cliente, sem carregar o
 * arquivo inteiro na memoria do storage-service no meio do caminho.
 * Upload continua carregando tudo de uma vez (o comando ja chega assim,
 * em base64, via RabbitMQ), mas o ponto que o plano pedia pra virar
 * streaming era especificamente o download.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupabaseServiceImpl implements SupabaseService {

    private final WebClient supabaseWebClient;
    private final SupabaseProperties supabaseProperties;

    @Override
    public Mono<Void> subir(byte[] conteudo, String contentType, BucketSupabase bucket, String nome) {

        log.debug("Upload Supabase bucket={} arquivo={}", bucket.getBucket(), nome);

        return supabaseWebClient.post()
                .uri("/storage/v1/object/{bucket}/{nome}", bucket.getBucket(), nome)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseProperties.getKey())
                .header("apikey", supabaseProperties.getKey())
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(contentType))
                .bodyValue(conteudo)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(
                        new IntegracaoExternaException(
                                "Falha ao subir arquivo para o Supabase (status: " + response.statusCode() + ")")))
                .toBodilessEntity()
                .then()
                .onErrorMap(this::naoEExcecaoConhecida, this::comoIntegracaoExterna);

    }

    @Override
    public Mono<ResponseEntity<Flux<DataBuffer>>> baixar(String nome, BucketSupabase bucket) {

        return supabaseWebClient.get()
                .uri("/storage/v1/object/{bucket}/{nome}", bucket.getBucket(), nome)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseProperties.getKey())
                .header("apikey", supabaseProperties.getKey())
                // retrieve().toEntityFlux (e nao exchangeToMono): o corpo e consumido DEPOIS, pelo servidor ao
                // responder o cliente; com exchangeToMono a conexao ja tinha sido liberada e o download saia vazio.
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), response -> Mono.error(
                        new RecursoNaoEncontradoException("Arquivo não encontrado no Supabase")))
                .toEntityFlux(DataBuffer.class)
                .map(entidade -> ResponseEntity.ok()
                        .contentType(entidade.getHeaders().getContentType() != null
                                ? entidade.getHeaders().getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nome + "\"")
                        .body(entidade.getBody()))
                .onErrorMap(this::naoEExcecaoConhecida, this::comoIntegracaoExterna);

    }

    @Override
    public Mono<Void> deletar(String nome, BucketSupabase bucket) {

        return supabaseWebClient.delete()
                .uri("/storage/v1/object/{bucket}/{nome}", bucket.getBucket(), nome)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseProperties.getKey())
                .header("apikey", supabaseProperties.getKey())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(
                        new IntegracaoExternaException(
                                "Falha ao deletar arquivo no Supabase (status: " + response.statusCode() + ")")))
                .toBodilessEntity()
                .then()
                .onErrorMap(this::naoEExcecaoConhecida, this::comoIntegracaoExterna);

    }

    private boolean naoEExcecaoConhecida(Throwable ex) {
        return !(ex instanceof RecursoNaoEncontradoException) && !(ex instanceof IntegracaoExternaException);
    }

    private Throwable comoIntegracaoExterna(Throwable ex) {
        return new IntegracaoExternaException("Falha ao comunicar com o Supabase: " + ex.getMessage());
    }

}
