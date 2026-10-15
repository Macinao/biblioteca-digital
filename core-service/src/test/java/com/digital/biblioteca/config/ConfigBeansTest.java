package com.digital.biblioteca.config;

import com.digital.biblioteca.config.security.SecurityConfig;
import com.digital.smartteca.common.events.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigBeansTest {

    // ---------- JacksonConfig ----------

    @Test
    void objectMapperSerializaInstantComoTextoISOEDesserializaDeVolta() throws Exception {
        ObjectMapper mapper = new JacksonConfig().objectMapper();
        DomainEvent evento = new DomainEvent("e1", "Livro", "1", "LivroCadastrado", "{}",
                Instant.parse("2026-03-10T08:15:30Z"), "corr");

        String json = mapper.writeValueAsString(evento);

        assertThat(mapper.readValue(json, DomainEvent.class)).isEqualTo(evento);
    }

    // ---------- KafkaTopicConfig ----------

    @Test
    void declaraUmTopicoPorAgregadoComTresParticoesEUmaReplica() {
        KafkaTopicConfig config = new KafkaTopicConfig();

        List<NewTopic> topicos = List.of(
                config.cursoEventsTopic(), config.temaEventsTopic(), config.livroEventsTopic(),
                config.livroPublicoEventsTopic(), config.tccEventsTopic(),
                config.materialEstudoEventsTopic(), config.reservaEventsTopic());

        assertThat(topicos).extracting(NewTopic::name).containsExactlyInAnyOrder(
                "curso-events", "tema-events", "livro-events", "livro-publico-events",
                "tcc-events", "material-estudo-events", "reserva-events");
        assertThat(topicos).allSatisfy(t -> {
            assertThat(t.numPartitions()).isEqualTo(3);
            assertThat(t.replicationFactor()).isEqualTo((short) 1);
        });
    }

    // ---------- KafkaProducerConfig ----------

    @Test
    void producerFactoryUsaServidorConfiguradoESerializadoresDeString() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "broker:9092");

        ProducerFactory<String, String> factory = config.producerFactory();

        Map<String, Object> props = factory.getConfigurationProperties();
        assertThat(props.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("broker:9092");
        assertThat(props.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)).isEqualTo(StringSerializer.class);
        assertThat(props.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG)).isEqualTo(StringSerializer.class);
    }

    @Test
    void kafkaTemplateUsaAProducerFactoryRecebida() {
        KafkaProducerConfig config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "broker:9092");
        ProducerFactory<String, String> factory = config.producerFactory();

        KafkaTemplate<String, String> template = config.kafkaTemplate(factory);

        assertThat(template.getProducerFactory()).isSameAs(factory);
    }

    // ---------- OpenApiConfig ----------

    @Test
    void openApiDescreveASmartTecaNaVersaoV1() {
        OpenAPI openApi = new OpenApiConfig().smartTecaOpenAPI();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("SmartTeca API");
        assertThat(openApi.getInfo().getVersion()).isEqualTo("v1");
        assertThat(openApi.getInfo().getDescription()).contains("reservas");
    }

    // ---------- SecurityConfig (beans) ----------

    @Test
    void passwordEncoderEBcryptEHasheiaComSaltAleatorio() {
        PasswordEncoder encoder = new SecurityConfig(null, null).passwordEncoder();

        String hashA = encoder.encode("Senha1234");
        String hashB = encoder.encode("Senha1234");

        assertThat(hashA).startsWith("$2a$").isNotEqualTo("Senha1234").isNotEqualTo(hashB);
        assertThat(encoder.matches("Senha1234", hashA)).isTrue();
        assertThat(encoder.matches("senha1234", hashA)).isFalse();
    }
}
