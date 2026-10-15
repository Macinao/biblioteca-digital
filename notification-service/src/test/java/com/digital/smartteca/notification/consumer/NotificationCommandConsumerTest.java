package com.digital.smartteca.notification.consumer;

import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.notification.service.MetodosMensagem;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationCommandConsumerTest {

    final MetodosMensagem metodosMensagem = mock(MetodosMensagem.class);
    final NotificationCommandConsumer consumer = new NotificationCommandConsumer(metodosMensagem, new ObjectMapper());

    static final String COMANDO = "{\"numero\":\"5583999998888\",\"mensagem\":\"Livro atrasado\"}";

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void comandoValidoEnviaOWhatsappComONumeroEATextoDoComando() throws Exception {
        when(metodosMensagem.enviarMensagem("5583999998888", "Livro atrasado")).thenReturn(Mono.just("ok"));

        consumer.consumirEnvioWhatsapp(COMANDO, "corr-1");

        verify(metodosMensagem).enviarMensagem("5583999998888", "Livro atrasado");
    }

    @Test
    void esperaOEnvioTerminarAntesDeConfirmarAMensagem() throws Exception {
        AtomicReference<Boolean> concluido = new AtomicReference<>(false);
        when(metodosMensagem.enviarMensagem(anyString(), anyString()))
                .thenReturn(Mono.just("ok").delayElement(java.time.Duration.ofMillis(200)).doOnSuccess(v -> concluido.set(true)));

        consumer.consumirEnvioWhatsapp(COMANDO, null);

        assertThat(concluido.get()).isTrue();
    }

    @Test
    void oCorrelationIdDaMensagemFicaNoMdcDuranteOEnvioESaiDepois() throws Exception {
        AtomicReference<String> mdcDuranteOEnvio = new AtomicReference<>();
        when(metodosMensagem.enviarMensagem(anyString(), anyString())).thenAnswer(inv -> {
            mdcDuranteOEnvio.set(MDC.get(CorrelationId.MDC_KEY));
            return Mono.just("ok");
        });

        consumer.consumirEnvioWhatsapp(COMANDO, "corr-da-reserva");

        assertThat(mdcDuranteOEnvio.get()).isEqualTo("corr-da-reserva");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void semCorrelationIdNoHeaderGeraUmEProcessaNormalmente() throws Exception {
        AtomicReference<String> mdcDuranteOEnvio = new AtomicReference<>();
        when(metodosMensagem.enviarMensagem(anyString(), anyString())).thenAnswer(inv -> {
            mdcDuranteOEnvio.set(MDC.get(CorrelationId.MDC_KEY));
            return Mono.just("ok");
        });

        consumer.consumirEnvioWhatsapp(COMANDO, null);

        assertThat(mdcDuranteOEnvio.get()).isNotBlank();
    }

    @Test
    void falhaNoEnvioPropagaAExcecaoParaOBrokerRejeitarEMandarParaAFilaMorta() {
        when(metodosMensagem.enviarMensagem(anyString(), anyString())).thenReturn(Mono.error(new IllegalStateException("evolution fora")));

        assertThatThrownBy(() -> consumer.consumirEnvioWhatsapp(COMANDO, "corr"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("evolution fora");

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void jsonInvalidoFalhaSemTentarEnviarNada() {
        assertThatThrownBy(() -> consumer.consumirEnvioWhatsapp("isto nao e json", "corr"))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);

        verify(metodosMensagem, never()).enviarMensagem(anyString(), anyString());
    }

    @Test
    void jsonComEstruturaErradaFalha() {
        assertThatThrownBy(() -> consumer.consumirEnvioWhatsapp("[1,2,3]", "corr"))
                .isInstanceOf(JsonMappingException.class);

        verify(metodosMensagem, never()).enviarMensagem(anyString(), anyString());
    }
}
