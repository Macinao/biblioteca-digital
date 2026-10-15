package com.digital.smartteca.search;

import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.repository.CursoDocumentRepository;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.repository.ReservaDocumentRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Endpoints de leitura e as queries de busca contra um Elasticsearch real. */
class SearchApiIT extends SearchBase {

    static final String TODOS = "BIBLIOTECARIO,PROFESSOR,ALUNO";

    @Autowired LivroDocumentRepository livros;
    @Autowired LivroPublicoDocumentRepository livrosPublicos;
    @Autowired TccDocumentRepository tccs;
    @Autowired MaterialEstudoDocumentRepository materiais;
    @Autowired CursoDocumentRepository cursos;
    @Autowired ReservaDocumentRepository reservas;
    @Autowired ObjectMapper objectMapper;

    private MockHttpServletRequestBuilder como(MockHttpServletRequestBuilder requisicao, String matricula, String roles) {
        return requisicao.header("X-User-Matricula", matricula).header("X-User-Roles", roles);
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private List<String> titulos(String caminho, String texto) throws Exception {
        MvcResult resultado = mvc.perform(como(get(caminho).param("texto", texto), "2023001", "ALUNO")).andReturn();
        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        List<String> titulos = new ArrayList<>();
        json(resultado).forEach(n -> titulos.add(n.get("titulo").asText()));
        return titulos;
    }

    private LivroDocument livro(String titulo, String autor, String descricao) {
        LivroDocument livro = new LivroDocument();
        livro.setId(UUID.randomUUID().toString());
        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setDescricao(descricao);
        livro.setQuantidade(2);
        livro.setDataPublicacao(LocalDateTime.of(2020, 1, 1, 0, 0));
        livro.setCursos(List.of());
        livro.setTemas(List.of());
        return livro;
    }

    // =====================  LISTAR  =====================

    @Test
    void listarDevolveTodosOsLivrosIndexadosComIdsUuid() throws Exception {
        livros.saveAll(List.of(livro("Clean Code", "Robert Martin", "Boas práticas"), livro("Refactoring", "Martin Fowler", "Melhorando código")));
        refresh(LivroDocument.class);

        MvcResult resultado = mvc.perform(como(get("/api/v1/livro/listar"), "2023001", "ALUNO")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        JsonNode corpo = json(resultado);
        assertThat(corpo).hasSize(2);
        corpo.forEach(n -> assertThat(UUID.fromString(n.get("id").asText())).isNotNull());
    }

    @Test
    void listarSemDadosDevolveListaVazia() throws Exception {
        MvcResult resultado = mvc.perform(como(get("/api/v1/livro/listar"), "2023001", "ALUNO")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(resultado)).isEmpty();
    }

    @Test
    void cadaIndiceTemSeuEndpointDeListagem() throws Exception {
        livrosPublicos.save(publico("Dom Casmurro", "Machado de Assis", "Romance"));
        TccDocument tcc = new TccDocument();
        tcc.setId(UUID.randomUUID().toString());
        tcc.setTitulo("Redes Neurais");
        tccs.save(tcc);
        MaterialEstudoDocument material = new MaterialEstudoDocument();
        material.setId(UUID.randomUUID().toString());
        material.setTitulo("Lista 1");
        materiais.save(material);
        cursos.save(curso(UUID.randomUUID().toString(), "Direito"));
        refresh(LivroPublicoDocument.class, TccDocument.class, MaterialEstudoDocument.class, com.digital.smartteca.search.document.CursoDocument.class);

        for (String caminho : List.of("/api/v1/livro-publico/listar", "/api/v1/tcc/listar", "/api/v1/material-estudo/listar", "/api/v1/curso/listar")) {
            MvcResult resultado = mvc.perform(como(get(caminho), "2023001", "ALUNO")).andReturn();
            assertThat(resultado.getResponse().getStatus()).as(caminho).isEqualTo(200);
            assertThat(json(resultado)).as(caminho).hasSize(1);
        }
    }

    private LivroPublicoDocument publico(String titulo, String autor, String descricao) {
        LivroPublicoDocument livro = new LivroPublicoDocument();
        livro.setId(UUID.randomUUID().toString());
        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setDescricao(descricao);
        return livro;
    }

    // =====================  BUSCAR (livro)  =====================

    @Test
    void buscaPorTituloEncontraOLivro() throws Exception {
        livros.saveAll(List.of(livro("Clean Code", "Robert Martin", "Boas práticas"), livro("Algoritmos", "Cormen", "Introdução")));
        refresh(LivroDocument.class);

        assertThat(titulos("/api/v1/livro/buscar", "Clean Code")).containsExactly("Clean Code");
    }

    @Test
    void buscaToleraErroDeDigitacaoEIgnoraCaixa() throws Exception {
        livros.save(livro("Clean Code", "Robert Martin", "Boas práticas"));
        refresh(LivroDocument.class);

        assertThat(titulos("/api/v1/livro/buscar", "clen cod")).contains("Clean Code");
        assertThat(titulos("/api/v1/livro/buscar", "CLEAN")).contains("Clean Code");
    }

    @Test
    void buscaPorAutorEPorDescricao() throws Exception {
        livros.saveAll(List.of(livro("Livro A", "Machado de Assis", "Romance clássico"), livro("Livro B", "Outro Autor", "Estudo sobre redes de computadores")));
        refresh(LivroDocument.class);

        assertThat(titulos("/api/v1/livro/buscar", "Machado")).containsExactly("Livro A");
        assertThat(titulos("/api/v1/livro/buscar", "redes")).containsExactly("Livro B");
    }

    @Test
    void tituloPesaMaisQueDescricaoNoRanking() throws Exception {
        livros.saveAll(List.of(
                livro("Introdução a Banco de Dados", "Autor X", "Conceitos básicos"),
                livro("Livro Qualquer", "Autor Y", "Este livro fala bastante sobre banco de dados relacionais")));
        refresh(LivroDocument.class);

        List<String> resultado = titulos("/api/v1/livro/buscar", "banco de dados");

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0)).isEqualTo("Introdução a Banco de Dados");
    }

    @Test
    void buscaPeloNomeDoCursoENomeDoTemaUsandoOsCamposNested() throws Exception {
        LivroDocument computacao = livro("Livro de Computação", "A", "x");
        computacao.setCursos(List.of(curso(UUID.randomUUID().toString(), "Ciência da Computação")));
        LivroDocument direito = livro("Livro de Direito", "B", "y");
        direito.setCursos(List.of(curso(UUID.randomUUID().toString(), "Direito")));
        direito.setTemas(List.of(tema(UUID.randomUUID().toString(), "Constitucional")));
        livros.saveAll(List.of(computacao, direito));
        refresh(LivroDocument.class);

        assertThat(titulos("/api/v1/livro/buscar", "Direito")).contains("Livro de Direito").doesNotContain("Livro de Computação");
        assertThat(titulos("/api/v1/livro/buscar", "Constitucional")).containsExactly("Livro de Direito");
    }

    @Test
    void analisadorBrasileiroCasaSingularEPlural() throws Exception {
        livros.save(livro("Estruturas de Dados", "Autor", "x"));
        refresh(LivroDocument.class);

        assertThat(titulos("/api/v1/livro/buscar", "estrutura")).contains("Estruturas de Dados");
        assertThat(titulos("/api/v1/livro/buscar", "dado")).contains("Estruturas de Dados");
    }

    @Test
    void semCorrespondenciaDevolveListaVazia() throws Exception {
        livros.save(livro("Clean Code", "Robert Martin", "Boas práticas"));
        refresh(LivroDocument.class);

        assertThat(titulos("/api/v1/livro/buscar", "xyzqwerty")).isEmpty();
    }

    @Test
    void buscaDevolveNoMaximoVinteResultados() throws Exception {
        List<LivroDocument> muitos = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            muitos.add(livro("Java Volume " + i, "Autor", "x"));
        }
        livros.saveAll(muitos);
        refresh(LivroDocument.class);

        assertThat(titulos("/api/v1/livro/buscar", "Java")).hasSize(20);
    }

    @Test
    void buscaSemOParametroTextoRetorna400() throws Exception {
        MvcResult resultado = mvc.perform(como(get("/api/v1/livro/buscar"), "2023001", "ALUNO")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void textoComCaracteresEspeciaisDeQueryNaoQuebraABusca() throws Exception {
        livros.save(livro("C++ Avançado", "Autor", "x"));
        refresh(LivroDocument.class);

        MvcResult resultado = mvc.perform(como(get("/api/v1/livro/buscar").param("texto", "c++ (avançado) \"AND\" * ? \\"), "2023001", "ALUNO")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
    }

    // =====================  BUSCAR (demais)  =====================

    @Test
    void buscaDeTccConsideraTituloOCursoUnicoEOsTemas() throws Exception {
        TccDocument porTitulo = new TccDocument();
        porTitulo.setId(UUID.randomUUID().toString());
        porTitulo.setTitulo("Sistema de reconhecimento facial");
        TccDocument porCurso = new TccDocument();
        porCurso.setId(UUID.randomUUID().toString());
        porCurso.setTitulo("Outro trabalho");
        porCurso.setCurso(curso(UUID.randomUUID().toString(), "Enfermagem"));
        TccDocument porTema = new TccDocument();
        porTema.setId(UUID.randomUUID().toString());
        porTema.setTitulo("Terceiro trabalho");
        porTema.setTemas(List.of(tema(UUID.randomUUID().toString(), "Gestão hospitalar")));
        tccs.saveAll(List.of(porTitulo, porCurso, porTema));
        refresh(TccDocument.class);

        assertThat(titulos("/api/v1/tcc/buscar", "reconhecimento")).containsExactly("Sistema de reconhecimento facial");
        assertThat(titulos("/api/v1/tcc/buscar", "Enfermagem")).containsExactly("Outro trabalho");
        assertThat(titulos("/api/v1/tcc/buscar", "hospitalar")).containsExactly("Terceiro trabalho");
    }

    @Test
    void buscaDeLivroPublicoEMaterialDeEstudo() throws Exception {
        livrosPublicos.save(publico("Dom Casmurro", "Machado de Assis", "Romance"));
        MaterialEstudoDocument material = new MaterialEstudoDocument();
        material.setId(UUID.randomUUID().toString());
        material.setTitulo("Lista de exercícios de cálculo");
        material.setCursos(List.of(curso(UUID.randomUUID().toString(), "Engenharia")));
        materiais.save(material);
        refresh(LivroPublicoDocument.class, MaterialEstudoDocument.class);

        assertThat(titulos("/api/v1/livro-publico/buscar", "Casmurro")).containsExactly("Dom Casmurro");
        assertThat(titulos("/api/v1/material-estudo/buscar", "Engenharia")).containsExactly("Lista de exercícios de cálculo");
    }

    // =====================  RESERVAS  =====================

    private ReservaDocument reserva(String matricula, String titulo, StatusReserva status) {
        ReservaDocument reserva = new ReservaDocument();
        reserva.setId(UUID.randomUUID().toString());
        reserva.setLivroId(UUID.randomUUID().toString());
        reserva.setTituloLivro(titulo);
        reserva.setMatriculaUsuario(matricula);
        reserva.setNomeUsuario("Usuario " + matricula);
        reserva.setStatusReserva(status);
        reserva.setDataCriacao(LocalDateTime.of(2026, 1, 1, 9, 0));
        reserva.setDataDevolucao(LocalDateTime.of(2026, 1, 8, 9, 0));
        return reserva;
    }

    @Test
    void bibliotecarioListaTodasAsReservasEAlunoNao() throws Exception {
        reservas.saveAll(List.of(reserva("2023001", "A", StatusReserva.RESERVADO), reserva("2023002", "B", StatusReserva.ATRASADO)));
        refresh(ReservaDocument.class);

        MvcResult bibliotecario = mvc.perform(como(get("/api/v1/reserva/listar"), "BIB0001", TODOS)).andReturn();
        MvcResult aluno = mvc.perform(como(get("/api/v1/reserva/listar"), "2023001", "ALUNO")).andReturn();

        assertThat(bibliotecario.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(bibliotecario)).hasSize(2);
        assertThat(aluno.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void listarMinhasDevolveSoAsReservasDaMatriculaAutenticada() throws Exception {
        reservas.saveAll(List.of(
                reserva("2023001", "Do aluno 1 - a", StatusReserva.RESERVADO),
                reserva("2023001", "Do aluno 1 - b", StatusReserva.ATRASADO),
                reserva("2023002", "Do aluno 2", StatusReserva.RESERVADO)));
        refresh(ReservaDocument.class);

        MvcResult resultado = mvc.perform(como(get("/api/v1/reserva/listar-minhas"), "2023001", "ALUNO")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        List<String> titulos = new ArrayList<>();
        json(resultado).forEach(n -> titulos.add(n.get("tituloLivro").asText()));
        assertThat(titulos).containsExactlyInAnyOrder("Do aluno 1 - a", "Do aluno 1 - b");
    }

    @Test
    void listarMinhasNaoMisturaMatriculasQueDiferemSoPelaCaixa() throws Exception {
        // Matricula e identificador exato (no Postgres "AB1234" e "ab1234" sao usuarios distintos).
        reservas.saveAll(List.of(
                reserva("AB1234", "Reserva de AB1234", StatusReserva.RESERVADO),
                reserva("ab1234", "Reserva de ab1234", StatusReserva.RESERVADO)));
        refresh(ReservaDocument.class);

        MvcResult resultado = mvc.perform(como(get("/api/v1/reserva/listar-minhas"), "AB1234", "ALUNO")).andReturn();

        List<String> titulos = new ArrayList<>();
        json(resultado).forEach(n -> titulos.add(n.get("tituloLivro").asText()));
        assertThat(titulos).containsExactly("Reserva de AB1234");
    }

    @Test
    void listarMinhasDeQuemNaoTemReservasDevolveVazio() throws Exception {
        reservas.save(reserva("2023002", "De outro", StatusReserva.RESERVADO));
        refresh(ReservaDocument.class);

        MvcResult resultado = mvc.perform(como(get("/api/v1/reserva/listar-minhas"), "2023001", "ALUNO")).andReturn();

        assertThat(json(resultado)).isEmpty();
    }

    // =====================  SEGURANCA  =====================

    @Test
    void semIdentidadeDoGatewayNaoHaAcessoEPapelDesconhecidoRecebe403() throws Exception {
        int semHeaders = mvc.perform(get("/api/v1/livro/listar")).andReturn().getResponse().getStatus();
        int papelDesconhecido = mvc.perform(como(get("/api/v1/livro/listar"), "2023001", "VISITANTE")).andReturn().getResponse().getStatus();

        assertThat(semHeaders).isIn(401, 403);
        assertThat(papelDesconhecido).isEqualTo(403);
    }

    @Test
    void professorEBibliotecarioComAHierarquiaDePapeisTambemConsultam() throws Exception {
        assertThat(mvc.perform(como(get("/api/v1/livro/listar"), "PROF001", "PROFESSOR,ALUNO")).andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(mvc.perform(como(get("/api/v1/livro/listar"), "BIB0001", TODOS)).andReturn().getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void falhaInternaDevolve500EmJsonSemVazarDetalheENaoUm403Vazio() throws Exception {
        // Simula o Elasticsearch sem o indice: a consulta lanca dentro do controller.
        operations.indexOps(LivroDocument.class).delete();

        MvcResult resultado = mvc.perform(como(get("/api/v1/livro/listar"), "2023001", "ALUNO")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(500);
        JsonNode erro = json(resultado);
        assertThat(erro.get("message").asText()).isEqualTo("Erro interno do servidor");
        assertThat(erro.get("path").asText()).isEqualTo("/api/v1/livro/listar");
        assertThat(resultado.getResponse().getContentAsString()).doesNotContainIgnoringCase("elasticsearch", "index_not_found");
    }

    @Test
    void rotaNaoMapeadaEhNegadaEHealthEPublico() throws Exception {
        assertThat(mvc.perform(como(get("/qualquer"), "2023001", "ALUNO")).andReturn().getResponse().getStatus()).isIn(401, 403, 404);
        assertThat(mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus()).isIn(200, 503);
    }
}
