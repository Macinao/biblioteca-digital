package com.digital.smartteca.common;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.EnviarWhatsappCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.smartteca.common.events.DomainEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Esses records viajam como JSON entre servicos (Kafka e RabbitMQ). O teste garante que o
 * que um servico publica e exatamente o que o outro desserializa.
 */
class ContratosJsonTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void domainEventSobreviveAIdaEVoltaEmJsonInclusiveOInstant() throws Exception {
        DomainEvent original = new DomainEvent("evt-1", "Livro", "42", "LivroCriado",
                "{\"titulo\":\"Clean Code\"}", Instant.parse("2026-01-15T10:30:00Z"), "corr-1");

        DomainEvent lido = mapper.readValue(mapper.writeValueAsString(original), DomainEvent.class);

        assertEquals(original, lido);
    }

    @Test
    void domainEventAceitaCorrelationIdNulo() throws Exception {
        DomainEvent original = new DomainEvent("evt-2", "Curso", "7", "CursoExcluido", "{}", Instant.EPOCH, null);

        DomainEvent lido = mapper.readValue(mapper.writeValueAsString(original), DomainEvent.class);

        assertEquals(original, lido);
    }

    @Test
    void uploadCommandPreservaOConteudoBase64EOBucket() throws Exception {
        UploadArquivoCommand original = new UploadArquivoCommand(
                BucketSupabase.LIVRO_CAPA, "capa.png", "image/png", "aGVsbG8=");

        UploadArquivoCommand lido = mapper.readValue(mapper.writeValueAsString(original), UploadArquivoCommand.class);

        assertEquals(original, lido);
    }

    @Test
    void bucketViajaComoNomeDoEnumNoJson() throws Exception {
        String json = mapper.writeValueAsString(new DeletarArquivoCommand(BucketSupabase.TCC_CONTEUDO, "tcc.pdf"));

        assertTrue(json.contains("\"TCC_CONTEUDO\""));
        assertEquals(BucketSupabase.TCC_CONTEUDO, mapper.readValue(json, DeletarArquivoCommand.class).bucket());
    }

    @Test
    void whatsappCommandSobreviveAIdaEVolta() throws Exception {
        EnviarWhatsappCommand original = new EnviarWhatsappCommand("5583999999999", "Sua reserva está atrasada.");

        EnviarWhatsappCommand lido = mapper.readValue(mapper.writeValueAsString(original), EnviarWhatsappCommand.class);

        assertEquals(original, lido);
    }

    @Test
    void nomesDeBucketDoSupabaseSaoUnicosEEmKebabCase() {
        Set<String> nomes = new HashSet<>();
        Arrays.stream(BucketSupabase.values()).forEach(b -> {
            assertTrue(b.getBucket().matches("[a-z]+(-[a-z]+)*"), b.getBucket());
            assertTrue(nomes.add(b.getBucket()), "bucket repetido: " + b.getBucket());
        });
    }

    @Test
    void statusDeReservaTemExatamenteOsValoresCompartilhadosComOSearchService() {
        assertEquals(Set.of("RESERVADO", "ATRASADO"),
                Arrays.stream(StatusReserva.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }
}
