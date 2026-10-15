package com.digital.smartteca.storage.consumer;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.storage.service.SupabaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageCommandConsumerTest {

    final SupabaseService supabase = mock(SupabaseService.class);
    final ObjectMapper objectMapper = new ObjectMapper();
    final StorageCommandConsumer consumer = new StorageCommandConsumer(supabase, objectMapper);

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    private String upload(String base64) throws Exception {
        return objectMapper.writeValueAsString(
                new UploadArquivoCommand(BucketSupabase.LIVRO_CAPA, "capa-1", "image/png", base64));
    }

    // ---------- upload ----------

    @Test
    void uploadDecodificaOBase64EChamaOSupabaseComBucketNomeETipo() throws Exception {
        byte[] original = "conteudo-binario".getBytes();
        when(supabase.subir(any(), anyString(), any(), anyString())).thenReturn(Mono.empty());

        consumer.consumirUpload(upload(Base64.getEncoder().encodeToString(original)), "corr-1");

        verify(supabase).subir(eq(original), eq("image/png"), eq(BucketSupabase.LIVRO_CAPA), eq("capa-1"));
    }

    @Test
    void uploadEsperaOSupabaseTerminarAntesDeConfirmar() throws Exception {
        AtomicReference<Boolean> concluido = new AtomicReference<>(false);
        when(supabase.subir(any(), anyString(), any(), anyString()))
                .thenReturn(Mono.<Void>empty().delaySubscription(java.time.Duration.ofMillis(200)).doOnSuccess(v -> concluido.set(true)));

        consumer.consumirUpload(upload("AAAA"), null);

        assertThat(concluido.get()).isTrue();
    }

    @Test
    void falhaNoSupabasePropagaParaAMensagemIrParaAFilaMorta() throws Exception {
        when(supabase.subir(any(), anyString(), any(), anyString())).thenReturn(Mono.error(new IllegalStateException("supabase fora")));

        assertThatThrownBy(() -> consumer.consumirUpload(upload("AAAA"), "corr"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void base64InvalidoFalhaSemChamarOSupabase() throws Exception {
        assertThatThrownBy(() -> consumer.consumirUpload(upload("%%%nao-e-base64%%%"), "corr"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(supabase, never()).subir(any(), anyString(), any(), anyString());
    }

    @Test
    void jsonInvalidoDeUploadFalhaSemChamarOSupabase() {
        assertThatThrownBy(() -> consumer.consumirUpload("nao e json", "corr"))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);

        verify(supabase, never()).subir(any(), anyString(), any(), anyString());
    }

    @Test
    void bucketDesconhecidoNoJsonFalha() {
        String json = "{\"bucket\":\"BUCKET_QUE_NAO_EXISTE\",\"nome\":\"x\",\"contentType\":\"image/png\",\"conteudoBase64\":\"AAAA\"}";

        assertThatThrownBy(() -> consumer.consumirUpload(json, "corr"))
                .isInstanceOf(com.fasterxml.jackson.databind.exc.InvalidFormatException.class);
    }

    @Test
    void correlationIdFicaNoMdcDuranteOUploadESaiDepois() throws Exception {
        AtomicReference<String> mdc = new AtomicReference<>();
        when(supabase.subir(any(), anyString(), any(), anyString())).thenAnswer(inv -> {
            mdc.set(MDC.get(CorrelationId.MDC_KEY));
            return Mono.empty();
        });

        consumer.consumirUpload(upload("AAAA"), "corr-do-livro");

        assertThat(mdc.get()).isEqualTo("corr-do-livro");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    // ---------- delete ----------

    @Test
    void deleteChamaOSupabaseComNomeEBucket() throws Exception {
        when(supabase.deletar(anyString(), any())).thenReturn(Mono.empty());
        String json = objectMapper.writeValueAsString(new DeletarArquivoCommand(BucketSupabase.TCC_CONTEUDO, "tcc-1"));

        consumer.consumirDelete(json, "corr");

        verify(supabase).deletar("tcc-1", BucketSupabase.TCC_CONTEUDO);
    }

    @Test
    void falhaNoDeletePropagaParaAFilaMorta() throws Exception {
        when(supabase.deletar(anyString(), any())).thenReturn(Mono.error(new IllegalStateException("falha")));
        String json = objectMapper.writeValueAsString(new DeletarArquivoCommand(BucketSupabase.TCC_CONTEUDO, "tcc-1"));

        assertThatThrownBy(() -> consumer.consumirDelete(json, "corr")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void jsonInvalidoDeDeleteFalhaSemChamarOSupabase() {
        assertThatThrownBy(() -> consumer.consumirDelete("{{", "corr"))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);

        verify(supabase, never()).deletar(anyString(), any());
    }
}
