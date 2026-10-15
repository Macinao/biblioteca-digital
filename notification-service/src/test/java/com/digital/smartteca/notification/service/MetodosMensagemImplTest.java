package com.digital.smartteca.notification.service;

import com.digital.smartteca.notification.EvolutionFake;
import com.digital.smartteca.notification.config.EvolutionProperties;
import com.digital.smartteca.notification.config.WebClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cliente da Evolution API contra um servidor HTTP real (fake). */
class MetodosMensagemImplTest {

    EvolutionFake evolution;
    MetodosMensagemImpl service;

    @BeforeEach
    void setUp() {
        evolution = new EvolutionFake();
        EvolutionProperties props = new EvolutionProperties();
        props.setUrl(evolution.url());
        props.setApikey("chave-secreta");
        props.setInstance("Biblioteca");
        service = new MetodosMensagemImpl(new WebClientConfig(props).evolutionWebClient(), props);
    }

    @AfterEach
    void tearDown() {
        evolution.parar();
    }

    @Test
    void enviaPostParaOEndpointDaInstanciaComApikeyEJsonComNumeroETexto() throws Exception {
        service.enviarMensagem("5583999998888", "Sua reserva está atrasada").block();

        assertThat(evolution.recebidas).hasSize(1);
        var recebida = evolution.recebidas.peek();
        assertThat(recebida.metodo()).isEqualTo("POST");
        assertThat(recebida.caminho()).isEqualTo("/message/sendText/Biblioteca");
        assertThat(recebida.apikey()).isEqualTo("chave-secreta");
        assertThat(recebida.contentType()).startsWith("application/json");
        JsonNode corpo = new ObjectMapper().readTree(recebida.corpo());
        assertThat(corpo.get("number").asText()).isEqualTo("5583999998888");
        assertThat(corpo.get("text").asText()).isEqualTo("Sua reserva está atrasada");
    }

    @Test
    void devolveOCorpoDaRespostaDaEvolution() {
        String resposta = service.enviarMensagem("5583999998888", "oi").block();

        assertThat(resposta).contains("PENDING");
    }

    @Test
    void usaOQueEstiverConfiguradoComoInstancia() {
        EvolutionProperties props = new EvolutionProperties();
        props.setUrl(evolution.url());
        props.setApikey("k");
        props.setInstance("OutraInstancia");
        MetodosMensagemImpl outro = new MetodosMensagemImpl(new WebClientConfig(props).evolutionWebClient(), props);

        outro.enviarMensagem("1", "x").block();

        assertThat(evolution.recebidas.peek().caminho()).isEqualTo("/message/sendText/OutraInstancia");
    }

    @Test
    void erroHttpDaEvolutionPropagaComoExcecaoParaAMensagemIrParaAFilaMorta() {
        evolution.status = 500;

        assertThatThrownBy(() -> service.enviarMensagem("5583999998888", "oi").block())
                .isInstanceOf(WebClientResponseException.InternalServerError.class);
    }

    @Test
    void numeroInvalidoRejeitadoPelaEvolutionCom400Propaga() {
        evolution.status = 400;

        assertThatThrownBy(() -> service.enviarMensagem("abc", "oi").block())
                .isInstanceOf(WebClientResponseException.BadRequest.class);
    }

    @Test
    void textoComCaracteresEspeciaisViajaIntacto() throws Exception {
        String texto = "Olá, João! O livro 'Clean Code' está atrasado — devolva logo. 📚";

        service.enviarMensagem("5583999998888", texto).block();

        JsonNode corpo = new ObjectMapper().readTree(evolution.recebidas.peek().corpo());
        assertThat(corpo.get("text").asText()).isEqualTo(texto);
    }
}
