package com.digital.smartteca.search;

import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.document.TemaDocument;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.kafka.KafkaContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/** Elasticsearch 9.4 e Kafka reais, compartilhados por todas as classes de integracao do search-service. */
@SpringBootTest
public abstract class SearchBase {

    static final ElasticsearchContainer ES = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.4.2")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withStartupTimeout(Duration.ofMinutes(4));
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.0");

    static {
        ES.start();
        KAFKA.start();
        try (AdminClient admin = AdminClient.create(Map.<String, Object>of("bootstrap.servers", KAFKA.getBootstrapServers()))) {
            admin.createTopics(List.of("curso-events", "tema-events", "livro-events", "livro-publico-events",
                    "tcc-events", "material-estudo-events", "reserva-events").stream().map(t -> new NewTopic(t, 1, (short) 1)).toList())
                    .all().get();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", () -> "http://" + ES.getHttpHostAddress());
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired protected WebApplicationContext context;
    @Autowired protected ElasticsearchOperations operations;

    protected MockMvc mvc;

    @BeforeEach
    void prepararBase() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        for (Class<?> tipo : List.of(CursoDocument.class, TemaDocument.class, LivroDocument.class, LivroPublicoDocument.class,
                TccDocument.class, MaterialEstudoDocument.class, ReservaDocument.class)) {
            var indice = operations.indexOps(tipo);
            if (indice.exists()) {
                indice.delete();
            }
            indice.createWithMapping();
        }
    }

    protected void refresh(Class<?>... tipos) {
        for (Class<?> tipo : tipos) {
            operations.indexOps(tipo).refresh();
        }
    }

    protected static CursoDocument curso(String id, String nome) {
        CursoDocument curso = new CursoDocument();
        curso.setId(id);
        curso.setNome(nome);
        return curso;
    }

    protected static TemaDocument tema(String id, String nome) {
        TemaDocument tema = new TemaDocument();
        tema.setId(id);
        tema.setNome(nome);
        return tema;
    }
}
