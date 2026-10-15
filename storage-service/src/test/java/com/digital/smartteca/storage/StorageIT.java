package com.digital.smartteca.storage;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** storage-service completo: HTTP (download) e RabbitMQ real (upload/delete) contra um Supabase falso. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StorageIT {

    static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:3.13-management");
    static final SupabaseFake SUPABASE = new SupabaseFake();

    static {
        RABBIT.start();
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBIT::getHost);
        registry.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registry.add("spring.supabase.url", SUPABASE::url);
        registry.add("spring.supabase.key", () -> "chave-it");
    }

    @LocalServerPort int porta;
    @Autowired RabbitTemplate rabbit;
    @Autowired ObjectMapper objectMapper;
    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + porta).build();
        SUPABASE.limpar();
        rabbit.execute(channel -> {
            for (String fila : new String[]{"storage.upload", "storage.delete", "storage.upload.dlq", "storage.delete.dlq"}) {
                channel.queuePurge(fila);
            }
            return null;
        });
    }

    private WebTestClient.RequestHeadersSpec<?> baixar(String caminho, String roles) {
        var spec = client.get().uri(caminho);
        return roles == null ? spec : spec.header("X-User-Matricula", "2023001").header("X-User-Roles", roles);
    }

    private void publicar(String fila, String json) {
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setHeader("X-Correlation-Id", "corr-it");
        rabbit.send(fila, MessageBuilder.withBody(json.getBytes()).andProperties(props).build());
    }

    private void aguardar(java.util.function.BooleanSupplier condicao) throws InterruptedException {
        long limite = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        while (!condicao.getAsBoolean() && System.currentTimeMillis() < limite) {
            Thread.sleep(100);
        }
    }

    // ---------- download pela API ----------

    @Test
    void alunoBaixaACapaDeLivroEORecebeOBinarioComContentType() {
        byte[] capa = "bytes-da-capa".getBytes();
        SUPABASE.arquivos.put("/storage/v1/object/livro-capa/capa-1", capa);
        SUPABASE.tiposDeConteudo.put("/storage/v1/object/livro-capa/capa-1", "image/png");

        client.get().uri("/api/v1/livro/baixar/capa/capa-1").header("X-User-Matricula", "2023001").header("X-User-Roles", "ALUNO")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectHeader().valueEquals("Content-Disposition", "inline; filename=\"capa-1\"")
                .expectBody(byte[].class).isEqualTo(capa);
    }

    @Test
    void cadaRotaDeDownloadLeDoBucketCorreto() {
        String[][] rotas = {
                {"/api/v1/livro/baixar/capa/a", "livro-capa"},
                {"/api/v1/livro-publico/baixar/capa/b", "livro-publico-capa"},
                {"/api/v1/livro-publico/baixar/conteudo/c", "livro-publico-conteudo"},
                {"/api/v1/material-estudo/baixar/conteudo/d", "material-conteudo"},
                {"/api/v1/tcc/baixar/conteudo/e", "tcc-conteudo"}};
        for (String[] rota : rotas) {
            String nome = rota[0].substring(rota[0].lastIndexOf('/') + 1);
            SUPABASE.arquivos.put("/storage/v1/object/" + rota[1] + "/" + nome, rota[1].getBytes());
            baixar(rota[0], "ALUNO").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo(rota[1]);
        }
    }

    @Test
    void arquivoInexistenteRetorna404EmJson() {
        baixar("/api/v1/tcc/baixar/conteudo/nao-existe", "ALUNO").exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.message").isEqualTo("Recurso não encontrado");
    }

    @Test
    void semAutenticacaoDoGatewayRetorna401ENadaVaiAoSupabase() {
        baixar("/api/v1/tcc/baixar/conteudo/x", null).exchange().expectStatus().isUnauthorized();

        assertThat(SUPABASE.recebidas).isEmpty();
    }

    @Test
    void papelSemPermissaoDeAlunoNaoBaixa() {
        baixar("/api/v1/tcc/baixar/conteudo/x", "VISITANTE").exchange().expectStatus().isForbidden();

        assertThat(SUPABASE.recebidas).isEmpty();
    }

    @Test
    void bibliotecarioEProfessorTambemBaixamQuandoOGatewayEnviaAHierarquiaDePapeis() {
        SUPABASE.arquivos.put("/storage/v1/object/tcc-conteudo/x", "pdf".getBytes());

        baixar("/api/v1/tcc/baixar/conteudo/x", "BIBLIOTECARIO,PROFESSOR,ALUNO").exchange().expectStatus().isOk();
        baixar("/api/v1/tcc/baixar/conteudo/x", "PROFESSOR,ALUNO").exchange().expectStatus().isOk();
    }

    @Test
    void rotaNaoMapeadaEhNegada() {
        baixar("/qualquer", "ALUNO").exchange().expectStatus().isForbidden();
    }

    @Test
    void healthEPublicoECorrelationIdVoltaNaResposta() {
        client.get().uri("/actuator/health").header("X-Correlation-Id", "corr-health").exchange()
                .expectHeader().valueEquals("X-Correlation-Id", "corr-health");
    }

    // ---------- upload e delete pela fila ----------

    @Test
    void comandoDeUploadNaFilaSobeOArquivoNoSupabaseComConteudoIntacto() throws Exception {
        byte[] conteudo = "conteudo-pdf-do-tcc".getBytes();
        publicar("storage.upload", objectMapper.writeValueAsString(new UploadArquivoCommand(
                BucketSupabase.TCC_CONTEUDO, "tcc-42", "application/pdf", Base64.getEncoder().encodeToString(conteudo))));

        aguardar(() -> SUPABASE.arquivos.containsKey("/storage/v1/object/tcc-conteudo/tcc-42"));

        assertThat(SUPABASE.arquivos.get("/storage/v1/object/tcc-conteudo/tcc-42")).isEqualTo(conteudo);
        assertThat(SUPABASE.recebidas.peek().authorization()).isEqualTo("Bearer chave-it");
        assertThat(rabbit.receive("storage.upload.dlq", 1_000)).isNull();
    }

    @Test
    void comandoDeDeleteNaFilaRemoveOArquivoDoSupabase() throws Exception {
        SUPABASE.arquivos.put("/storage/v1/object/livro-capa/capa-9", "x".getBytes());
        publicar("storage.delete", objectMapper.writeValueAsString(new DeletarArquivoCommand(BucketSupabase.LIVRO_CAPA, "capa-9")));

        aguardar(() -> !SUPABASE.arquivos.containsKey("/storage/v1/object/livro-capa/capa-9"));

        assertThat(SUPABASE.arquivos).doesNotContainKey("/storage/v1/object/livro-capa/capa-9");
    }

    @Test
    void supabaseComErroMandaOUploadParaAFilaMortaSemReprocessar() throws Exception {
        SUPABASE.statusForcado = 500;
        publicar("storage.upload", objectMapper.writeValueAsString(new UploadArquivoCommand(
                BucketSupabase.LIVRO_CAPA, "capa-1", "image/png", Base64.getEncoder().encodeToString("x".getBytes()))));

        Message morta = rabbit.receive("storage.upload.dlq", 10_000);

        assertThat(morta).isNotNull();
        Thread.sleep(1500);
        assertThat(SUPABASE.recebidas).as("uma unica tentativa, sem requeue").hasSize(1);
    }

    @Test
    void base64OuJsonInvalidosVaoParaAFilaMortaSemChamarOSupabase() throws Exception {
        publicar("storage.upload", "{\"bucket\":\"LIVRO_CAPA\",\"nome\":\"x\",\"contentType\":\"image/png\",\"conteudoBase64\":\"%%%\"}");
        publicar("storage.upload", "isto nao e json");

        Message primeira = rabbit.receive("storage.upload.dlq", 10_000);
        Message segunda = rabbit.receive("storage.upload.dlq", 10_000);

        assertThat(primeira).isNotNull();
        assertThat(segunda).isNotNull();
        assertThat(SUPABASE.recebidas).isEmpty();
    }

    @Test
    void deleteDeArquivoInexistenteVaiParaAFilaMortaDeDelete() throws Exception {
        publicar("storage.delete", objectMapper.writeValueAsString(new DeletarArquivoCommand(BucketSupabase.LIVRO_CAPA, "nao-existe")));

        assertThat(rabbit.receive("storage.delete.dlq", 10_000)).isNotNull();
    }
}
