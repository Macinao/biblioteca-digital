package com.digital.smartteca.chatbot;

import com.digital.smartteca.chatbot.memoria.MemoriaService;
import com.digital.smartteca.chatbot.memoria.Mensagem;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * chatbot-service completo: Redis real, um "LLM" falso (API compativel com OpenAI) que pede uma tool, e um
 * search-service falso que responde a essa tool.
 */
@SpringBootTest
class ChatbotIT {

    record Recebida(String metodo, String caminhoComQuery, Map<String, String> headers, String corpo) {
    }

    static class Fake {
        final HttpServer server;
        final ConcurrentLinkedQueue<Recebida> recebidas = new ConcurrentLinkedQueue<>();
        volatile Function<Recebida, String[]> resposta; // {status, contentType, corpo}

        Fake(Function<Recebida, String[]> resposta) {
            this.resposta = resposta;
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            server.createContext("/", exchange -> {
                String corpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> headers = new java.util.HashMap<>();
                exchange.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(), v.get(0)));
                Recebida recebida = new Recebida(exchange.getRequestMethod(), exchange.getRequestURI().toString(), headers, corpo);
                recebidas.add(recebida);
                String[] r = this.resposta.apply(recebida);
                byte[] bytes = r[2].getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", r[1]);
                exchange.sendResponseHeaders(Integer.parseInt(r[0]), bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }
    }

    static String completionComTool(String nomeDaTool, String argumentosJson) {
        return """
                {"id":"chatcmpl-1","object":"chat.completion","created":1,"model":"qwen3:8b",
                 "choices":[{"index":0,"finish_reason":"tool_calls","message":{"role":"assistant","content":null,
                   "tool_calls":[{"id":"call_1","type":"function","function":{"name":"%s","arguments":%s}}]}}],
                 "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}"""
                .formatted(nomeDaTool, quote(argumentosJson));
    }

    static String completionComTexto(String texto) {
        return """
                {"id":"chatcmpl-2","object":"chat.completion","created":2,"model":"qwen3:8b",
                 "choices":[{"index":0,"finish_reason":"stop","message":{"role":"assistant","content":%s}}],
                 "usage":{"prompt_tokens":20,"completion_tokens":8,"total_tokens":28}}""".formatted(quote(texto));
    }

    static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    /** O "modelo": na 1a chamada pede a tool buscarLivro; depois que a tool respondeu, devolve texto citando o resultado. */
    static String[] llmComTool(Recebida chamada) {
        if (chamada.corpo().contains("\"role\":\"tool\"") || chamada.corpo().contains("\"role\": \"tool\"")) {
            return new String[]{"200", "application/json", completionComTexto("Encontrei o livro Clean Code no acervo.")};
        }
        return new String[]{"200", "application/json", completionComTool("buscarLivro", "{\"busca\":\"clean code\"}")};
    }

    static final String LIVROS_JSON = "[{\"titulo\":\"Clean Code\",\"autor\":\"Robert Martin\",\"descricao\":\"Boas práticas\","
            + "\"cursos\":[{\"id\":\"1\",\"nome\":\"Computação\"}],\"temas\":[{\"id\":\"2\",\"nome\":\"Software\"}]}]";

    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
    static final Fake LLM = new Fake(ChatbotIT::llmComTool);
    static final Fake SEARCH = new Fake(r -> new String[]{"200", "application/json", LIVROS_JSON});

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.ai.openai.base-url", () -> LLM.url() + "/v1");
        registry.add("spring.ai.openai.api-key", () -> "chave-it");
        registry.add("services.search-url", SEARCH::url);
    }

    @Autowired WebApplicationContext context;
    @Autowired StringRedisTemplate redis;
    @Autowired MemoriaService memoria;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        redis.getConnectionFactory().getConnection().serverCommands().flushAll();
        LLM.recebidas.clear();
        SEARCH.recebidas.clear();
        LLM.resposta = ChatbotIT::llmComTool;
        SEARCH.resposta = r -> new String[]{"200", "application/json", LIVROS_JSON};
    }

    private MvcResult enviar(String matricula, String roles, String mensagem) throws Exception {
        var requisicao = post("/api/v1/chatbot/mensagem").contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensagem\":\"" + mensagem + "\"}");
        if (matricula != null) {
            requisicao.header("X-User-Matricula", matricula).header("X-User-Roles", roles);
        }
        return mvc.perform(requisicao).andReturn();
    }

    // ---------- fluxo completo ----------

    @Test
    void perguntaDoAlunoFazOModeloChamarATollQueBuscaNoSearchEResponderComOResultado() throws Exception {
        MvcResult resultado = enviar("2023001", "ALUNO", "Tem o livro Clean Code?");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("Encontrei o livro Clean Code no acervo.");

        assertThat(SEARCH.recebidas).hasSize(1);
        Recebida busca = SEARCH.recebidas.peek();
        assertThat(busca.caminhoComQuery()).isEqualTo("/api/v1/livro/buscar?texto=clean%20code");
        assertThat(busca.headers().get("x-user-matricula")).isEqualTo("2023001");
        assertThat(busca.headers().get("x-user-roles")).isEqualTo("ALUNO");

        assertThat(LLM.recebidas).hasSize(2);
        List<Recebida> chamadas = new ArrayList<>(LLM.recebidas);
        assertThat(chamadas.get(0).corpo()).contains("Tem o livro Clean Code?").contains("buscarLivro");
        assertThat(chamadas.get(1).corpo()).contains("Título: Clean Code").contains("Autor: Robert Martin");
    }

    @Test
    void oPromptDoModeloTrazAMatriculaEOSystemPromptDaSmartTeca() throws Exception {
        enviar("2023001", "ALUNO", "oi");

        String primeira = LLM.recebidas.peek().corpo();
        assertThat(primeira).contains("SmartTeca").contains("Usuário: 2023001");
    }

    @Test
    void pergunta_e_resposta_ficam_guardadas_no_redis_na_ordem() throws Exception {
        enviar("2023001", "ALUNO", "Tem o livro Clean Code?");

        List<Mensagem> guardadas = memoria.buscarUltimasMensagens("2023001", 10);

        assertThat(guardadas).extracting(Mensagem::getRole, Mensagem::getConteudo).containsExactly(
                org.assertj.core.groups.Tuple.tuple("usuario", "Tem o livro Clean Code?"),
                org.assertj.core.groups.Tuple.tuple("assistente", "Encontrei o livro Clean Code no acervo."));
    }

    @Test
    void asProximasPerguntasLevamOHistoricoDaConversaNoPrompt() throws Exception {
        enviar("2023001", "ALUNO", "primeira pergunta");
        LLM.recebidas.clear();

        enviar("2023001", "ALUNO", "segunda pergunta");

        String promptDaSegunda = LLM.recebidas.peek().corpo();
        assertThat(promptDaSegunda).contains("usuario: primeira pergunta").contains("assistente: Encontrei o livro Clean Code");
    }

    @Test
    void cadaUsuarioTemSuaPropriaMemoria() throws Exception {
        enviar("AAAA001", "ALUNO", "pergunta da A");
        enviar("BBBB001", "ALUNO", "pergunta da B");

        assertThat(memoria.buscarUltimasMensagens("AAAA001", 10)).extracting(Mensagem::getConteudo)
                .contains("pergunta da A").doesNotContain("pergunta da B");
        assertThat(memoria.buscarUltimasMensagens("BBBB001", 10)).extracting(Mensagem::getConteudo)
                .contains("pergunta da B").doesNotContain("pergunta da A");
    }

    @Test
    void modeloQueNaoUsaToolResponderDiretoSemChamarOSearch() throws Exception {
        LLM.resposta = r -> new String[]{"200", "application/json", completionComTexto("Bom dia! Como posso ajudar?")};

        MvcResult resultado = enviar("2023001", "ALUNO", "bom dia");

        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("Bom dia! Como posso ajudar?");
        assertThat(SEARCH.recebidas).isEmpty();
    }

    // ---------- tool_use_failed ----------

    @Test
    void toolUseFailedDoModeloFazUmaNovaTentativaERespondeNormalmente() throws Exception {
        java.util.concurrent.atomic.AtomicInteger chamadas = new java.util.concurrent.atomic.AtomicInteger();
        LLM.resposta = r -> chamadas.incrementAndGet() == 1
                ? new String[]{"400", "application/json", "{\"error\":{\"message\":\"tool_use_failed: invalid function call\",\"type\":\"invalid_request_error\"}}"}
                : new String[]{"200", "application/json", completionComTexto("Resposta após retry")};

        MvcResult resultado = enviar("2023001", "ALUNO", "pergunta");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).isEqualTo("Resposta após retry");
        assertThat(chamadas.get()).isEqualTo(2);
    }

    @Test
    void toolUseFailedPersistenteDevolveMensagemAmigavelEmVezDeErro500() throws Exception {
        LLM.resposta = r -> new String[]{"400", "application/json", "{\"error\":{\"message\":\"tool_use_failed\",\"type\":\"invalid_request_error\"}}"};

        MvcResult resultado = enviar("2023001", "ALUNO", "pergunta");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentAsString()).startsWith("Desculpe, tive um problema técnico");
    }

    @Test
    void modeloForaDoArDevolve503EmJsonSemVazarAUrlDoProvedorENaoUm403Vazio() throws Exception {
        LLM.resposta = r -> new String[]{"500", "application/json", "{\"error\":\"boom\"}"};

        MvcResult resultado = enviar("2023001", "ALUNO", "pergunta");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(503);
        assertThat(resultado.getResponse().getContentAsString())
                .contains("Assistente indisponível")
                .doesNotContain("127.0.0.1", "boom");
    }

    @Test
    void jsonQuebradoEErroDoClienteRetorna400E503SoParaFalhaDoModelo() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/chatbot/mensagem")
                        .header("X-User-Matricula", "2023001").header("X-User-Roles", "ALUNO")
                        .contentType(MediaType.APPLICATION_JSON).content("{nao e json"))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(LLM.recebidas).isEmpty();
    }

    // ---------- autorizacao e validacao ----------

    @Test
    void semIdentidadeDoGatewayNaoHaAcesso() throws Exception {
        MvcResult resultado = enviar(null, null, "oi");

        assertThat(resultado.getResponse().getStatus()).isIn(401, 403);
        assertThat(LLM.recebidas).isEmpty();
    }

    @Test
    void papelSemPermissaoDeAlunoRecebe403() throws Exception {
        MvcResult resultado = enviar("2023001", "VISITANTE", "oi");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(LLM.recebidas).isEmpty();
    }

    @Test
    void mensagemEmBrancoOuMaiorQueDoisMilCaracteresRetorna400() throws Exception {
        assertThat(enviar("2023001", "ALUNO", " ").getResponse().getStatus()).isEqualTo(400);
        assertThat(enviar("2023001", "ALUNO", "a".repeat(2001)).getResponse().getStatus()).isEqualTo(400);
        assertThat(enviar("2023001", "ALUNO", "a".repeat(2000)).getResponse().getStatus()).isEqualTo(200);
    }

    // ---------- memoria no redis (concorrencia) ----------

    @Test
    void quarentaMensagensSimultaneasDoMesmoUsuarioNaoPerdemNenhuma() throws Exception {
        int threads = 40;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch prontos = new CountDownLatch(threads);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            futures.add(executor.submit(() -> {
                prontos.countDown();
                largada.await();
                memoria.adicionarMensagem("CONC001", "usuario", "msg-" + n);
                return null;
            }));
        }
        prontos.await();
        largada.countDown();
        for (Future<?> f : futures) {
            f.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        List<Mensagem> todas = memoria.buscarUltimasMensagens("CONC001", 50);

        assertThat(todas).hasSize(40);
        assertThat(todas).extracting(Mensagem::getConteudo).doesNotHaveDuplicates();
    }

    @Test
    void memoriaGuardaNoMaximoAsUltimasCinquentaMensagensEPreservaAOrdem() {
        for (int i = 0; i < 60; i++) {
            memoria.adicionarMensagem("LIM0001", "usuario", "m" + i);
        }

        List<Mensagem> todas = memoria.buscarUltimasMensagens("LIM0001", 100);

        assertThat(todas).hasSize(50);
        assertThat(todas.get(0).getConteudo()).isEqualTo("m10");
        assertThat(todas.get(49).getConteudo()).isEqualTo("m59");
        assertThat(memoria.buscarUltimasMensagens("LIM0001", 3)).extracting(Mensagem::getConteudo)
                .containsExactly("m57", "m58", "m59");
    }
}
