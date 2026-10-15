package com.digital.smartteca.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Fluxo completo: RabbitMQ real -> consumer -> Evolution API (fake). */
@SpringBootTest
class NotificationIT {

    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");
    static final EvolutionFake EVOLUTION = new EvolutionFake();

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("spring.evolution.url", EVOLUTION::url);
        registry.add("spring.evolution.apikey", () -> "chave-it");
        registry.add("spring.evolution.instance", () -> "InstanciaIT");
    }

    @Autowired RabbitTemplate rabbit;

    @BeforeEach
    void limpar() {
        EVOLUTION.recebidas.clear();
        EVOLUTION.status = 201;
        EVOLUTION.falharProximas(0);
        rabbit.execute(channel -> {
            channel.queuePurge("notification.whatsapp.send");
            channel.queuePurge("notification.whatsapp.send.dlq");
            return null;
        });
    }

    private void publicar(String json, String correlationId) {
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        if (correlationId != null) {
            props.setHeader("X-Correlation-Id", correlationId);
        }
        Message mensagem = MessageBuilder.withBody(json.getBytes()).andProperties(props).build();
        rabbit.send("notification.whatsapp.send", mensagem);
    }

    private void aguardar(java.util.function.BooleanSupplier condicao) throws InterruptedException {
        long limite = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        while (!condicao.getAsBoolean() && System.currentTimeMillis() < limite) {
            Thread.sleep(100);
        }
    }

    private Message proximaDaFilaMorta() {
        return rabbit.receive("notification.whatsapp.send.dlq", 10_000);
    }

    @Test
    void comandoNaFilaViraUmPostNaEvolutionComApikeyEInstanciaConfiguradas() throws Exception {
        publicar("{\"numero\":\"5583999998888\",\"mensagem\":\"Seu livro está atrasado\"}", "corr-it");

        aguardar(() -> !EVOLUTION.recebidas.isEmpty());

        assertThat(EVOLUTION.recebidas).hasSize(1);
        var recebida = EVOLUTION.recebidas.peek();
        assertThat(recebida.caminho()).isEqualTo("/message/sendText/InstanciaIT");
        assertThat(recebida.apikey()).isEqualTo("chave-it");
        assertThat(recebida.corpo()).contains("5583999998888").contains("Seu livro está atrasado");
        assertThat(rabbit.receive("notification.whatsapp.send.dlq", 1_000)).isNull();
    }

    @Test
    void evolutionFalhandoMandaAMensagemParaAFilaMortaSemReprocessarEmLoop() throws Exception {
        EVOLUTION.status = 500;
        publicar("{\"numero\":\"5583999998888\",\"mensagem\":\"vai falhar\"}", "corr-falha");

        Message morta = proximaDaFilaMorta();

        assertThat(morta).isNotNull();
        assertThat(new String(morta.getBody())).contains("vai falhar");
        Thread.sleep(1500);
        assertThat(EVOLUTION.recebidas).as("tentou uma unica vez, sem requeue").hasSize(1);
    }

    @Test
    void jsonInvalidoVaiDiretoParaAFilaMortaSemChamarAEvolution() throws Exception {
        publicar("isto nao e json", null);

        Message morta = proximaDaFilaMorta();

        assertThat(morta).isNotNull();
        assertThat(new String(morta.getBody())).isEqualTo("isto nao e json");
        assertThat(EVOLUTION.recebidas).isEmpty();
    }

    @Test
    void umaMensagemComFalhaNaoImpedeAsSeguintes() throws Exception {
        EVOLUTION.falharProximas(1);
        publicar("{\"numero\":\"1\",\"mensagem\":\"primeira (falha)\"}", null);
        aguardar(() -> EVOLUTION.recebidas.size() >= 1);
        publicar("{\"numero\":\"2\",\"mensagem\":\"segunda (ok)\"}", null);

        aguardar(() -> EVOLUTION.recebidas.size() >= 2);

        assertThat(EVOLUTION.recebidas).hasSize(2);
        assertThat(EVOLUTION.recebidas).extracting(r -> r.corpo().contains("segunda")).contains(true);
        Message morta = proximaDaFilaMorta();
        assertThat(new String(morta.getBody())).contains("primeira (falha)");
    }

    @Test
    void variasMensagensSaoTodasEntreguesUmaPorComando() throws Exception {
        for (int i = 0; i < 10; i++) {
            publicar("{\"numero\":\"55839999900" + i + "\",\"mensagem\":\"msg " + i + "\"}", "corr-" + i);
        }

        aguardar(() -> EVOLUTION.recebidas.size() >= 10);

        assertThat(EVOLUTION.recebidas).hasSize(10);
        assertThat(EVOLUTION.recebidas).extracting(r -> r.corpo()).allSatisfy(c -> assertThat(c).contains("msg "));
    }
}
