package com.digital.biblioteca;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Endpoints de escrita do acervo, com seguranca, validacao, banco e outbox reais. */
class ApiAcervoIT extends IntegracaoBase {

    static final String HEADER = "X-User-Matricula";
    static final String BIB = "BIB0001";

    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void usuarios() {
        criarUsuario(BIB, Role.BIBLIOTECARIO);
    }

    // ---------- helpers ----------

    private String json(Object objeto) throws Exception {
        return objectMapper.writeValueAsString(objeto);
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private MvcResult enviarJson(HttpMethod metodo, String url, Object corpo, String matricula) throws Exception {
        var requisicao = metodo == HttpMethod.PUT ? put(url) : post(url);
        return mvc.perform(requisicao.header(HEADER, matricula).contentType(MediaType.APPLICATION_JSON).content(json(corpo))).andReturn();
    }

    private MockMultipartFile parteJson(String nome, Object corpo) throws Exception {
        return new MockMultipartFile(nome, "", "application/json", json(corpo).getBytes());
    }

    private static MockMultipartFile png(String nome) {
        return new MockMultipartFile(nome, "capa.png", "image/png", "imagem".getBytes());
    }

    private static MockMultipartFile pdf(String nome) {
        return new MockMultipartFile(nome, "arquivo.pdf", "application/pdf", "pdf".getBytes());
    }

    private MvcResult enviarMultipart(HttpMethod metodo, String url, String matricula, MockMultipartFile... partes) throws Exception {
        MockMultipartHttpServletRequestBuilder builder = multipart(metodo, url);
        for (MockMultipartFile parte : partes) {
            builder.file(parte);
        }
        return mvc.perform(builder.header(HEADER, matricula)).andReturn();
    }

    private List<String> eventosOutbox() {
        return jdbc.queryForList("SELECT event_type FROM outbox_event ORDER BY data_criacao, event_type", String.class);
    }

    private int contar(String tabela) {
        return jdbc.queryForObject("SELECT count(*) FROM " + tabela, Integer.class);
    }

    private Map<String, Object> livroRequest(Curso curso, Tema tema) {
        return Map.of("titulo", "Clean Code", "autor", "Robert Martin", "descricao", "Boas praticas",
                "temas", List.of(tema.getId()), "cursos", List.of(curso.getId()),
                "dataPublicacao", "2020-01-01T00:00:00", "quantidade", 3);
    }

    // =====================  CURSO  =====================

    @Test
    void cursoCadastrarRetorna201GravaECursoCadastradoNoOutbox() throws Exception {
        MvcResult resultado = enviarJson(HttpMethod.POST, "/api/v1/curso/cadastrar", Map.of("nome", "Direito"), BIB);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        assertThat(corpo(resultado).get("nome").asText()).isEqualTo("Direito");
        assertThat(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrue("Direito")).isTrue();
        assertThat(eventosOutbox()).containsExactlyInAnyOrder("CursoCadastrado");
    }

    @Test
    void cursoDuplicadoIgnorandoCaixaRetorna422() throws Exception {
        criarCurso("Direito");

        MvcResult resultado = enviarJson(HttpMethod.POST, "/api/v1/curso/cadastrar", Map.of("nome", "DIREITO"), BIB);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
        assertThat(contar("curso")).isEqualTo(1);
    }

    @Test
    void cursoSemNomeRetorna400ComOCampoNosDetalhes() throws Exception {
        MvcResult resultado = enviarJson(HttpMethod.POST, "/api/v1/curso/cadastrar", Map.of("nome", " "), BIB);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(corpo(resultado).get("details").asText()).contains("nome:");
    }

    @Test
    void cursoAtualizarTrocaONomeEPublicaEvento() throws Exception {
        Curso curso = criarCurso("Direito");

        MvcResult resultado = enviarJson(HttpMethod.PUT, "/api/v1/curso/atualizar",
                Map.of("cursoId", curso.getId(), "novoCurso", "Direito Penal"), BIB);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(cursoRepository.findById(curso.getId()).orElseThrow().getNome()).isEqualTo("Direito Penal");
        assertThat(eventosOutbox()).containsExactlyInAnyOrder("CursoAtualizado");
    }

    @Test
    void cursoAtualizarParaNomeDeOutroCursoRetorna422() throws Exception {
        Curso direito = criarCurso("Direito");
        criarCurso("Medicina");

        MvcResult resultado = enviarJson(HttpMethod.PUT, "/api/v1/curso/atualizar",
                Map.of("cursoId", direito.getId(), "novoCurso", "medicina"), BIB);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    void cursoAtualizarInexistenteRetorna404() throws Exception {
        MvcResult resultado = enviarJson(HttpMethod.PUT, "/api/v1/curso/atualizar",
                Map.of("cursoId", UUID.randomUUID(), "novoCurso", "Qualquer"), BIB);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void cursoDeletarFazExclusaoLogicaEPermiteReusarONome() throws Exception {
        Curso curso = criarCurso("Direito");

        mvc.perform(delete("/api/v1/curso/deletar/" + curso.getId()).header(HEADER, BIB)).andExpect(status().isNoContent());

        assertThat(cursoRepository.findById(curso.getId()).orElseThrow().isAtivo()).isFalse();
        assertThat(eventosOutbox()).containsExactlyInAnyOrder("CursoExcluido");
        assertThat(enviarJson(HttpMethod.POST, "/api/v1/curso/cadastrar", Map.of("nome", "Direito"), BIB).getResponse().getStatus())
                .isEqualTo(201);
    }

    @Test
    void cursoDeletarInexistenteRetorna404() throws Exception {
        mvc.perform(delete("/api/v1/curso/deletar/" + UUID.randomUUID()).header(HEADER, BIB)).andExpect(status().isNotFound());
    }

    // =====================  TEMA  =====================

    @Test
    void temaCadastrarAtualizarEDeletarFuncionamEPublicamEventos() throws Exception {
        MvcResult criado = enviarJson(HttpMethod.POST, "/api/v1/tema/cadastrar", Map.of("nome", "Algoritmos"), BIB);
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(corpo(criado).get("id").asText());

        assertThat(enviarJson(HttpMethod.PUT, "/api/v1/tema/atualizar", Map.of("temaId", id, "novoTema", "Estruturas"), BIB)
                .getResponse().getStatus()).isEqualTo(200);
        mvc.perform(delete("/api/v1/tema/deletar/" + id).header(HEADER, BIB)).andExpect(status().isNoContent());

        assertThat(eventosOutbox()).containsExactlyInAnyOrder("TemaAtualizado", "TemaCadastrado", "TemaExcluido");
        assertThat(temaRepository.findById(id).orElseThrow().isAtivo()).isFalse();
    }

    @Test
    void temaDuplicadoRetorna422EInexistenteRetorna404() throws Exception {
        criarTema("Algoritmos");

        assertThat(enviarJson(HttpMethod.POST, "/api/v1/tema/cadastrar", Map.of("nome", "algoritmos"), BIB).getResponse().getStatus()).isEqualTo(422);
        assertThat(enviarJson(HttpMethod.PUT, "/api/v1/tema/atualizar", Map.of("temaId", UUID.randomUUID(), "novoTema", "X"), BIB)
                .getResponse().getStatus()).isEqualTo(404);
        mvc.perform(delete("/api/v1/tema/deletar/" + UUID.randomUUID()).header(HEADER, BIB)).andExpect(status().isNotFound());
    }

    // =====================  LIVRO  =====================

    @Test
    void livroCadastrarRetorna201PersisteRelacoesEEnfileiraUploadDaCapaMaisOEventoDeDominio() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");

        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/livro/cadastrar", BIB,
                parteJson("livro", livroRequest(curso, tema)), png("capa"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        JsonNode corpo = corpo(resultado);
        assertThat(corpo.get("titulo").asText()).isEqualTo("Clean Code");
        assertThat(corpo.get("cursos").get(0).get("nome").asText()).isEqualTo("Computação");
        assertThat(corpo.get("temas").get(0).get("nome").asText()).isEqualTo("Algoritmos");
        assertThat(contar("livro_curso")).isEqualTo(1);
        assertThat(contar("material_tema")).isEqualTo(1);
        UUID id = UUID.fromString(corpo.get("id").asText());
        assertThat(livroRepository.findById(id).orElseThrow().getCapa()).isEqualTo(id.toString());
        assertThat(eventosOutbox()).containsExactlyInAnyOrder("ArquivoParaSubir", "LivroCadastrado");
        assertThat(jdbc.queryForObject("SELECT broker FROM outbox_event WHERE event_type = 'ArquivoParaSubir'", String.class)).isEqualTo("RABBITMQ");
        assertThat(jdbc.queryForObject("SELECT destination FROM outbox_event WHERE event_type = 'ArquivoParaSubir'", String.class)).isEqualTo("storage.upload");
    }

    @Test
    void livroCadastrarComCapaQueNaoEImagemRetorna400SemGravarNada() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");

        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/livro/cadastrar", BIB,
                parteJson("livro", livroRequest(curso, tema)), pdf("capa"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(contar("livro")).isZero();
        assertThat(eventosOutbox()).isEmpty();
    }

    @Test
    void livroCadastrarSemCapaRetorna400() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");

        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/livro/cadastrar", BIB, parteJson("livro", livroRequest(curso, tema)));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(contar("livro")).isZero();
    }

    @Test
    void livroCadastrarComDadosInvalidosRetorna400() throws Exception {
        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/livro/cadastrar", BIB,
                parteJson("livro", Map.of("titulo", "")), png("capa"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(contar("livro")).isZero();
    }

    @Test
    void livroAtualizarSemCapaTrocaOsDadosEMantemOIdEACapa() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");
        Livro livro = criarLivro("Original", 1);
        Map<String, Object> pedido = new java.util.HashMap<>(livroRequest(curso, tema));
        pedido.put("id", livro.getId());
        pedido.put("titulo", "Titulo novo");
        pedido.put("quantidade", 8);

        MvcResult resultado = enviarMultipart(HttpMethod.PUT, "/api/v1/livro/atualizar", BIB, parteJson("livro", pedido));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        Livro lido = livroRepository.findById(livro.getId()).orElseThrow();
        assertThat(lido.getTitulo()).isEqualTo("Titulo novo");
        assertThat(lido.getQuantidade()).isEqualTo(8);
        assertThat(lido.getCapa()).isEqualTo("capa-Original");
        assertThat(eventosOutbox()).containsExactlyInAnyOrder("LivroAtualizado");
    }

    @Test
    void livroAtualizarComNovaCapaEnfileiraUpload() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");
        Livro livro = criarLivro("Original", 1);
        Map<String, Object> pedido = new java.util.HashMap<>(livroRequest(curso, tema));
        pedido.put("id", livro.getId());

        enviarMultipart(HttpMethod.PUT, "/api/v1/livro/atualizar", BIB, parteJson("livro", pedido), png("capa"));

        assertThat(eventosOutbox()).containsExactlyInAnyOrder("ArquivoParaSubir", "LivroAtualizado");
    }

    @Test
    void livroAtualizarInexistenteRetorna404() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");
        Map<String, Object> pedido = new java.util.HashMap<>(livroRequest(curso, tema));
        pedido.put("id", UUID.randomUUID());

        MvcResult resultado = enviarMultipart(HttpMethod.PUT, "/api/v1/livro/atualizar", BIB, parteJson("livro", pedido));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void livroDeletarFazExclusaoLogicaEPedeAExclusaoDaCapa() throws Exception {
        Livro livro = criarLivro("Para apagar", 1);

        mvc.perform(delete("/api/v1/livro/deletar/" + livro.getId()).header(HEADER, BIB)).andExpect(status().isNoContent());

        assertThat(livroRepository.findById(livro.getId()).orElseThrow().isAtivo()).isFalse();
        assertThat(eventosOutbox()).containsExactlyInAnyOrder("ArquivoParaExcluir", "LivroExcluido");
    }

    @Test
    void livroComReservaAtivaNaoPodeSerDeletado() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Reservado", 2);
        enviarJson(HttpMethod.POST, "/api/v1/reserva/reservar", Map.of("usuario", "2023001", "livro", livro.getId()), BIB);

        mvc.perform(delete("/api/v1/livro/deletar/" + livro.getId()).header(HEADER, BIB)).andExpect(status().isUnprocessableEntity());

        assertThat(livroRepository.findById(livro.getId()).orElseThrow().isAtivo()).isTrue();
    }

    // =====================  TCC  =====================

    private Map<String, Object> tccRequest(Curso curso, Tema tema) {
        return Map.of("titulo", "Sistema de biblioteca", "autor", "Joao", "descricao", "TCC",
                "temas", List.of(tema.getId()), "curso", curso.getId(), "dataPublicacao", "2024-06-01T00:00:00");
    }

    @Test
    void tccCadastrarAtualizarEDeletarFuncionamComUploadDoPdf() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Software");

        MvcResult criado = enviarMultipart(HttpMethod.POST, "/api/v1/tcc/cadastrar", BIB, parteJson("tcc", tccRequest(curso, tema)), pdf("conteudo"));
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(corpo(criado).get("id").asText());
        assertThat(corpo(criado).get("curso").get("nome").asText()).isEqualTo("Computação");

        Map<String, Object> atualizacao = new java.util.HashMap<>(tccRequest(curso, tema));
        atualizacao.put("id", id);
        atualizacao.put("titulo", "Novo titulo");
        assertThat(enviarMultipart(HttpMethod.PUT, "/api/v1/tcc/atualizar", BIB, parteJson("tcc", atualizacao)).getResponse().getStatus()).isEqualTo(200);
        mvc.perform(delete("/api/v1/tcc/deletar/" + id).header(HEADER, BIB)).andExpect(status().isNoContent());

        assertThat(eventosOutbox()).containsExactlyInAnyOrder("ArquivoParaSubir", "TccCadastrado", "TccAtualizado", "ArquivoParaExcluir", "TccExcluido");
    }

    @Test
    void tccComConteudoQueNaoEPdfRetorna400() throws Exception {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Software");

        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/tcc/cadastrar", BIB, parteJson("tcc", tccRequest(curso, tema)), png("conteudo"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(contar("tcc")).isZero();
    }

    @Test
    void tccComCursoInexistenteRetorna404ENaoDeixaLinhaNemEventoPorCausaDoRollback() throws Exception {
        Tema tema = criarTema("Software");
        Curso fantasma = new Curso();
        fantasma.setId(UUID.randomUUID());

        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/tcc/cadastrar", BIB,
                parteJson("tcc", tccRequest(fantasma, tema)), pdf("conteudo"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(contar("tcc")).isZero();
        assertThat(contar("material_academico")).isZero();
        assertThat(eventosOutbox()).isEmpty();
    }

    // =====================  LIVRO PUBLICO  =====================

    @Test
    void livroPublicoCadastrarEnfileiraDoisUploadsEDeletarPedeDuasExclusoes() throws Exception {
        Curso curso = criarCurso("Letras");
        Tema tema = criarTema("Romance");
        Map<String, Object> pedido = Map.of("titulo", "Dom Casmurro", "autor", "Machado", "descricao", "Romance",
                "cursos", List.of(curso.getId()), "temas", List.of(tema.getId()), "dataPublicacao", "1899-01-01T00:00:00");

        MvcResult criado = enviarMultipart(HttpMethod.POST, "/api/v1/livro-publico/cadastrar", BIB,
                parteJson("livro-publico", pedido), pdf("conteudo"), png("capa"));
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(corpo(criado).get("id").asText());
        mvc.perform(delete("/api/v1/livro-publico/deletar/" + id).header(HEADER, BIB)).andExpect(status().isNoContent());

        assertThat(eventosOutbox()).containsExactlyInAnyOrder("ArquivoParaSubir", "ArquivoParaSubir", "LivroPublicoCadastrado",
                "ArquivoParaExcluir", "ArquivoParaExcluir", "LivroPublicoExcluido");
    }

    @Test
    void livroPublicoSemCapaRetorna400() throws Exception {
        Curso curso = criarCurso("Letras");
        Tema tema = criarTema("Romance");
        Map<String, Object> pedido = Map.of("titulo", "Dom Casmurro", "autor", "Machado", "descricao", "Romance",
                "cursos", List.of(curso.getId()), "temas", List.of(tema.getId()), "dataPublicacao", "1899-01-01T00:00:00");

        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/livro-publico/cadastrar", BIB,
                parteJson("livro-publico", pedido), pdf("conteudo"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    // =====================  MATERIAL DE ESTUDO  =====================

    private Map<String, Object> materialRequest(Curso curso, Tema tema) {
        return Map.of("titulo", "Lista 1", "autor", "Prof. Ana", "descricao", "Exercicios",
                "temas", List.of(tema.getId()), "cursos", List.of(curso.getId()), "dataPublicacao", "2025-03-01T00:00:00");
    }

    @Test
    void professorCadastraMaterialEViraOCriadorEApenasEleOuOBibliotecarioPodemAltera_lo() throws Exception {
        Usuario ana = criarUsuario("PROF001", Role.PROFESSOR);
        criarUsuario("PROF002", Role.PROFESSOR);
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");

        MvcResult criado = enviarMultipart(HttpMethod.POST, "/api/v1/material-estudo/cadastrar", "PROF001",
                parteJson("material-estudo", materialRequest(curso, tema)), pdf("conteudo"));
        assertThat(criado.getResponse().getStatus()).isEqualTo(201);
        UUID id = UUID.fromString(corpo(criado).get("id").asText());
        assertThat(jdbc.queryForObject("SELECT criador_id FROM material_estudo", UUID.class)).isEqualTo(ana.getId());

        Map<String, Object> atualizacao = new java.util.HashMap<>(materialRequest(curso, tema));
        atualizacao.put("id", id);
        assertThat(enviarMultipart(HttpMethod.PUT, "/api/v1/material-estudo/atualizar", "PROF002", parteJson("material-estudo", atualizacao))
                .getResponse().getStatus()).isEqualTo(403);
        mvc.perform(delete("/api/v1/material-estudo/deletar/" + id).header(HEADER, "PROF002")).andExpect(status().isForbidden());

        assertThat(enviarMultipart(HttpMethod.PUT, "/api/v1/material-estudo/atualizar", "PROF001", parteJson("material-estudo", atualizacao))
                .getResponse().getStatus()).isEqualTo(200);
        assertThat(enviarMultipart(HttpMethod.PUT, "/api/v1/material-estudo/atualizar", BIB, parteJson("material-estudo", atualizacao))
                .getResponse().getStatus()).isEqualTo(200);
        mvc.perform(delete("/api/v1/material-estudo/deletar/" + id).header(HEADER, BIB)).andExpect(status().isNoContent());
    }

    @Test
    void alunoNaoCadastraMaterialDeEstudo() throws Exception {
        criarUsuario("ALUNO01", Role.ALUNO);
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");

        MvcResult resultado = enviarMultipart(HttpMethod.POST, "/api/v1/material-estudo/cadastrar", "ALUNO01",
                parteJson("material-estudo", materialRequest(curso, tema)), pdf("conteudo"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(403);
        assertThat(contar("material_estudo")).isZero();
    }

    // =====================  RESERVA  =====================

    @Test
    void bibliotecarioReservaEDevolveLivroPelaApi() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 1);

        MvcResult reservada = enviarJson(HttpMethod.POST, "/api/v1/reserva/reservar", Map.of("usuario", "2023001", "livro", livro.getId()), BIB);
        assertThat(reservada.getResponse().getStatus()).isEqualTo(201);
        JsonNode corpo = corpo(reservada);
        assertThat(corpo.get("tituloLivro").asText()).isEqualTo("Clean Code");
        assertThat(corpo.get("statusReserva").asText()).isEqualTo("RESERVADO");
        assertThat(livroRepository.findById(livro.getId()).orElseThrow().getQuantidade()).isZero();

        MvcResult devolvida = enviarJson(HttpMethod.PUT, "/api/v1/reserva/devolver", Map.of("reserva", corpo.get("id").asText()), BIB);
        assertThat(devolvida.getResponse().getStatus()).isEqualTo(204);
        assertThat(livroRepository.findById(livro.getId()).orElseThrow().getQuantidade()).isEqualTo(1);
    }

    @Test
    void reservaSemEstoqueRetorna422EDevolverInexistenteRetorna404() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        Livro esgotado = criarLivro("Esgotado", 0);

        assertThat(enviarJson(HttpMethod.POST, "/api/v1/reserva/reservar", Map.of("usuario", "2023001", "livro", esgotado.getId()), BIB)
                .getResponse().getStatus()).isEqualTo(422);
        assertThat(enviarJson(HttpMethod.PUT, "/api/v1/reserva/devolver", Map.of("reserva", UUID.randomUUID()), BIB)
                .getResponse().getStatus()).isEqualTo(404);
    }

    @Test
    void reservaComCorpoIncompletoRetorna400() throws Exception {
        MvcResult resultado = enviarJson(HttpMethod.POST, "/api/v1/reserva/reservar", Map.of("usuario", "2023001"), BIB);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }
}
