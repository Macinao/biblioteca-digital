package com.digital.smartteca.search;

import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.repository.ReservaDocumentRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** Paginação, total exato, filtro por curso e detalhe por id contra um Elasticsearch real. */
class PaginacaoIT extends SearchBase {

    @Autowired LivroDocumentRepository livros;
    @Autowired LivroPublicoDocumentRepository livrosPublicos;
    @Autowired TccDocumentRepository tccs;
    @Autowired MaterialEstudoDocumentRepository materiais;
    @Autowired ReservaDocumentRepository reservas;
    @Autowired ObjectMapper objectMapper;

    private MockHttpServletRequestBuilder aluno(String caminho) {
        return get(caminho).header("X-User-Matricula", "2023001").header("X-User-Roles", "ALUNO");
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private String total(MvcResult resultado) {
        return resultado.getResponse().getHeader("X-Total-Count");
    }

    private List<String> titulos(MvcResult resultado) throws Exception {
        List<String> titulos = new ArrayList<>();
        corpo(resultado).forEach(n -> titulos.add(n.get("titulo").asText()));
        return titulos;
    }

    /** Livro n tem data 2000+n: quanto maior o n, mais recente. */
    private LivroDocument livro(int n, CursoDocument... cursos) {
        LivroDocument livro = new LivroDocument();
        livro.setId(UUID.randomUUID().toString());
        livro.setTitulo("Livro " + n);
        livro.setAutor("Autor");
        livro.setDescricao("Descrição");
        livro.setQuantidade(1);
        livro.setDataPublicacao(LocalDateTime.of(2000 + n % 100, 1, 1, 0, 0).plusDays(n / 100));
        livro.setCursos(List.of(cursos));
        livro.setTemas(List.of());
        return livro;
    }

    private void indexarLivros(int quantidade) {
        List<LivroDocument> lote = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            lote.add(livro(i));
            if (lote.size() == 1000) {
                livros.saveAll(lote);
                lote.clear();
            }
        }
        livros.saveAll(lote);
        refresh(LivroDocument.class);
    }

    // =====================  paginação e total  =====================

    @Test
    void trazAPaginaPedidaOTotalNoHeaderEOrdenaDoMaisRecenteParaOMaisAntigo() throws Exception {
        indexarLivros(30);

        MvcResult primeira = mvc.perform(aluno("/api/v1/livro/listar").param("pagina", "0").param("tamanho", "10")).andReturn();
        MvcResult ultima = mvc.perform(aluno("/api/v1/livro/listar").param("pagina", "2").param("tamanho", "10")).andReturn();
        MvcResult alemDoFim = mvc.perform(aluno("/api/v1/livro/listar").param("pagina", "3").param("tamanho", "10")).andReturn();

        assertThat(primeira.getResponse().getStatus()).isEqualTo(200);
        assertThat(titulos(primeira)).containsExactly("Livro 29", "Livro 28", "Livro 27", "Livro 26", "Livro 25",
                "Livro 24", "Livro 23", "Livro 22", "Livro 21", "Livro 20");
        assertThat(total(primeira)).isEqualTo("30");
        assertThat(titulos(ultima)).containsExactly("Livro 9", "Livro 8", "Livro 7", "Livro 6", "Livro 5",
                "Livro 4", "Livro 3", "Livro 2", "Livro 1", "Livro 0");
        assertThat(corpo(alemDoFim)).isEmpty();
        assertThat(total(alemDoFim)).isEqualTo("30");
    }

    @Test
    void semParametrosUsaPaginaDe24Itens() throws Exception {
        indexarLivros(30);

        MvcResult resultado = mvc.perform(aluno("/api/v1/livro/listar")).andReturn();

        assertThat(corpo(resultado)).hasSize(24);
        assertThat(total(resultado)).isEqualTo("30");
    }

    @Test
    void totalContinuaExatoAcimaDeDezMilDocumentosEmVezDeTravarEmDezMil() throws Exception {
        // O Elasticsearch trunca o total em 10.000 se não for pedido track_total_hits: era um bug real.
        indexarLivros(10_050);

        MvcResult resultado = mvc.perform(aluno("/api/v1/livro/listar").param("tamanho", "1")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(total(resultado)).isEqualTo("10050");
    }

    @Test
    void reservasTambemSaoPaginadasComTotalExato() throws Exception {
        for (int i = 0; i < 5; i++) {
            ReservaDocument reserva = new ReservaDocument();
            reserva.setId(UUID.randomUUID().toString());
            reserva.setLivroId(UUID.randomUUID().toString());
            reserva.setTituloLivro("Reserva " + i);
            reserva.setMatriculaUsuario("2023001");
            reserva.setStatusReserva(StatusReserva.RESERVADO);
            reserva.setDataCriacao(LocalDateTime.of(2026, 1, 1 + i, 0, 0));
            reservas.save(reserva);
        }
        refresh(ReservaDocument.class);

        MvcResult resultado = mvc.perform(get("/api/v1/reserva/listar").param("pagina", "1").param("tamanho", "2")
                .header("X-User-Matricula", "BIB0001").header("X-User-Roles", "BIBLIOTECARIO,PROFESSOR,ALUNO")).andReturn();

        assertThat(total(resultado)).isEqualTo("5");
        List<String> titulos = new ArrayList<>();
        corpo(resultado).forEach(n -> titulos.add(n.get("tituloLivro").asText()));
        assertThat(titulos).containsExactly("Reserva 2", "Reserva 1"); // mais recentes primeiro: 4,3 | 2,1 | 0
    }

    // =====================  filtro por curso  =====================

    @Test
    void filtraPeloCursoEOTotalContaSoOsDoCurso() throws Exception {
        CursoDocument computacao = curso(UUID.randomUUID().toString(), "Computação");
        CursoDocument direito = curso(UUID.randomUUID().toString(), "Direito");
        for (int i = 0; i < 5; i++) livros.save(livro(i, computacao));
        for (int i = 5; i < 12; i++) livros.save(livro(i, direito));
        livros.save(livro(12, computacao, direito)); // em dois cursos: aparece nos dois filtros
        refresh(LivroDocument.class);

        MvcResult deComputacao = mvc.perform(aluno("/api/v1/livro/listar").param("cursoId", computacao.getId())).andReturn();
        MvcResult deDireito = mvc.perform(aluno("/api/v1/livro/listar").param("cursoId", direito.getId())).andReturn();
        MvcResult inexistente = mvc.perform(aluno("/api/v1/livro/listar").param("cursoId", UUID.randomUUID().toString())).andReturn();

        assertThat(total(deComputacao)).isEqualTo("6");
        assertThat(corpo(deComputacao)).hasSize(6);
        assertThat(total(deDireito)).isEqualTo("8");
        assertThat(corpo(inexistente)).isEmpty();
        assertThat(total(inexistente)).isEqualTo("0");
    }

    @Test
    void filtroPorCursoNaoCasaPorPedacoDoId() throws Exception {
        // O id do curso é um UUID; um filtro com só um pedaço dele (o analisador de texto o quebraria) não pode casar.
        CursoDocument curso = curso("465c0535-24b4-4ba1-a0f5-7f73c0262610", "Computação");
        livros.save(livro(1, curso));
        refresh(LivroDocument.class);

        MvcResult pedaco = mvc.perform(aluno("/api/v1/livro/listar").param("cursoId", "465c0535")).andReturn();
        MvcResult completo = mvc.perform(aluno("/api/v1/livro/listar").param("cursoId", curso.getId())).andReturn();

        assertThat(corpo(pedaco)).isEmpty();
        assertThat(corpo(completo)).hasSize(1);
    }

    @Test
    void tccFiltraPeloSeuCursoUnicoEOsOutrosTiposTambemAceitamOFiltro() throws Exception {
        CursoDocument direito = curso(UUID.randomUUID().toString(), "Direito");
        CursoDocument computacao = curso(UUID.randomUUID().toString(), "Computação");
        TccDocument tccDireito = new TccDocument();
        tccDireito.setId(UUID.randomUUID().toString());
        tccDireito.setTitulo("TCC de Direito");
        tccDireito.setCurso(direito);
        TccDocument tccComputacao = new TccDocument();
        tccComputacao.setId(UUID.randomUUID().toString());
        tccComputacao.setTitulo("TCC de Computação");
        tccComputacao.setCurso(computacao);
        tccs.saveAll(List.of(tccDireito, tccComputacao));
        LivroPublicoDocument publico = new LivroPublicoDocument();
        publico.setId(UUID.randomUUID().toString());
        publico.setTitulo("Público de Direito");
        publico.setCursos(List.of(direito));
        livrosPublicos.save(publico);
        MaterialEstudoDocument material = new MaterialEstudoDocument();
        material.setId(UUID.randomUUID().toString());
        material.setTitulo("Material de Computação");
        material.setCursos(List.of(computacao));
        materiais.save(material);
        refresh(TccDocument.class, LivroPublicoDocument.class, MaterialEstudoDocument.class);

        assertThat(titulos(mvc.perform(aluno("/api/v1/tcc/listar").param("cursoId", direito.getId())).andReturn())).containsExactly("TCC de Direito");
        assertThat(titulos(mvc.perform(aluno("/api/v1/livro-publico/listar").param("cursoId", direito.getId())).andReturn())).containsExactly("Público de Direito");
        assertThat(titulos(mvc.perform(aluno("/api/v1/material-estudo/listar").param("cursoId", computacao.getId())).andReturn())).containsExactly("Material de Computação");
        assertThat(corpo(mvc.perform(aluno("/api/v1/material-estudo/listar").param("cursoId", direito.getId())).andReturn())).isEmpty();
    }

    // =====================  parâmetros inválidos  =====================

    @ParameterizedTest
    @ValueSource(strings = {"pagina=-1", "tamanho=0", "tamanho=101", "pagina=abc", "tamanho=xyz", "pagina=100&tamanho=100"})
    void parametrosInvalidosRetornam400EmJson(String consulta) throws Exception {
        MvcResult resultado = mvc.perform(aluno("/api/v1/livro/listar?" + consulta)).andReturn();

        assertThat(resultado.getResponse().getStatus()).as(consulta).isEqualTo(400);
        assertThat(corpo(resultado).get("message").asText()).isEqualTo("Requisição inválida");
    }

    @Test
    void ultimaPosicaoPermitidaDaJanelaDoElasticsearchFunciona() throws Exception {
        indexarLivros(5);

        // 99 * 100 + 100 = 10.000: exatamente o limite
        MvcResult resultado = mvc.perform(aluno("/api/v1/livro/listar").param("pagina", "99").param("tamanho", "100")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(corpo(resultado)).isEmpty();
    }

    // =====================  detalhe por id  =====================

    @Test
    void detalheDevolveOItemDeCadaTipoEOrdenaOCursoDoTcc() throws Exception {
        CursoDocument direito = curso(UUID.randomUUID().toString(), "Direito");
        LivroDocument livro = livro(1, direito);
        livros.save(livro);
        TccDocument tcc = new TccDocument();
        tcc.setId(UUID.randomUUID().toString());
        tcc.setTitulo("Meu TCC");
        tcc.setCurso(direito);
        tccs.save(tcc);
        LivroPublicoDocument publico = new LivroPublicoDocument();
        publico.setId(UUID.randomUUID().toString());
        publico.setTitulo("Público");
        livrosPublicos.save(publico);
        MaterialEstudoDocument material = new MaterialEstudoDocument();
        material.setId(UUID.randomUUID().toString());
        material.setTitulo("Material");
        materiais.save(material);
        refresh(LivroDocument.class, TccDocument.class, LivroPublicoDocument.class, MaterialEstudoDocument.class);

        JsonNode doLivro = corpo(mvc.perform(aluno("/api/v1/livro/" + livro.getId())).andReturn());
        JsonNode doTcc = corpo(mvc.perform(aluno("/api/v1/tcc/" + tcc.getId())).andReturn());

        assertThat(doLivro.get("titulo").asText()).isEqualTo("Livro 1");
        assertThat(doLivro.get("cursos").get(0).get("nome").asText()).isEqualTo("Direito");
        assertThat(doTcc.get("curso").get("nome").asText()).isEqualTo("Direito");
        assertThat(corpo(mvc.perform(aluno("/api/v1/livro-publico/" + publico.getId())).andReturn()).get("titulo").asText()).isEqualTo("Público");
        assertThat(corpo(mvc.perform(aluno("/api/v1/material-estudo/" + material.getId())).andReturn()).get("titulo").asText()).isEqualTo("Material");
    }

    @ParameterizedTest
    @ValueSource(strings = {"livro", "livro-publico", "tcc", "material-estudo"})
    void detalheDeIdInexistenteRetorna404EmJson(String tipo) throws Exception {
        MvcResult resultado = mvc.perform(aluno("/api/v1/" + tipo + "/" + UUID.randomUUID())).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(404);
        assertThat(corpo(resultado).get("message").asText()).isEqualTo("Recurso não encontrado");
    }

    @Test
    void detalheExigeLoginENaoConfundeComAsRotasListarEBuscar() throws Exception {
        MvcResult semLogin = mvc.perform(get("/api/v1/livro/" + UUID.randomUUID())).andReturn();
        MvcResult listar = mvc.perform(aluno("/api/v1/livro/listar")).andReturn();

        assertThat(semLogin.getResponse().getStatus()).isIn(401, 403);
        assertThat(listar.getResponse().getStatus()).isEqualTo(200); // "listar" não foi tratado como um id
    }
}
