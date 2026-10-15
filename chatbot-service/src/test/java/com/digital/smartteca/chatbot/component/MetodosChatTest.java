package com.digital.smartteca.chatbot.component;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** As 4 tools do LLM: chamam o search-service via HTTP (aqui, um servidor simulado) e formatam o texto devolvido ao modelo. */
class MetodosChatTest {

    MockRestServiceServer search;
    MetodosChat tools;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://search");
        search = MockRestServiceServer.bindTo(builder).build();
        tools = new MetodosChat(builder.build());

        MockHttpServletRequest requisicaoOriginal = new MockHttpServletRequest();
        requisicaoOriginal.addHeader("X-User-Matricula", "2023001");
        requisicaoOriginal.addHeader("X-User-Roles", "ALUNO");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requisicaoOriginal));
    }

    @AfterEach
    void limpar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void esperaGet(String url, String json) {
        search.expect(requestTo(url)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    // ---------- buscarLivro ----------

    @Test
    void buscarLivroFormataTituloAutorDescricaoCursosETemas() {
        esperaGet("http://search/api/v1/livro/buscar?texto=clean",
                "[{\"titulo\":\"Clean Code\",\"autor\":\"Robert Martin\",\"descricao\":\"Boas práticas\","
                        + "\"cursos\":[{\"id\":\"1\",\"nome\":\"Computação\"},{\"id\":\"2\",\"nome\":\"Sistemas\"}],"
                        + "\"temas\":[{\"id\":\"3\",\"nome\":\"Software\"}]}]");

        String resultado = tools.buscarLivro("clean");

        assertThat(resultado).contains("Título: Clean Code", "Autor: Robert Martin", "Descrição: Boas práticas",
                "Cursos: Computação, Sistemas", "Temas: Software");
        search.verify();
    }

    @Test
    void buscarLivroSeparaVariosResultadosEUsaTextosPadraoParaCamposAusentes() {
        esperaGet("http://search/api/v1/livro/buscar?texto=x",
                "[{\"titulo\":\"A\",\"autor\":\"Autor A\"},{\"titulo\":\"B\",\"autor\":\"Autor B\",\"descricao\":\"d\"}]");

        String resultado = tools.buscarLivro("x");

        assertThat(resultado).contains("\n---\n");
        assertThat(resultado).contains("Descrição: Sem descrição", "Cursos: Nenhum curso vinculado", "Temas: Nenhum tema vinculado");
        assertThat(resultado.split("\n---\n")).hasSize(2);
    }

    @Test
    void buscarLivroSemResultadoDizQueNaoEncontrouComOTermoBuscado() {
        esperaGet("http://search/api/v1/livro/buscar?texto=inexistente", "[]");

        assertThat(tools.buscarLivro("inexistente")).isEqualTo("Nenhum livro encontrado no acervo para: inexistente");
    }

    @Test
    void identidadeDoUsuarioDaRequisicaoOriginalEhRepassadaAoSearchService() {
        search.expect(requestTo("http://search/api/v1/livro/buscar?texto=java"))
                .andExpect(header("X-User-Matricula", "2023001"))
                .andExpect(header("X-User-Roles", "ALUNO"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        tools.buscarLivro("java");

        search.verify();
    }

    @Test
    void textoComCaracteresEspeciaisVaiCodificadoNaQueryString() {
        search.expect(request -> assertThat(request.getURI().getRawQuery()).isEqualTo("texto=direito%20%26%20leis%3F%23"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        tools.buscarLivro("direito & leis?#");

        search.verify();
    }

    @Test
    void corpoNuloDoSearchServiceViraListaVaziaEMensagemDeNaoEncontrado() {
        search.expect(requestTo("http://search/api/v1/livro/buscar?texto=x")).andRespond(withSuccess());

        assertThat(tools.buscarLivro("x")).startsWith("Nenhum livro encontrado");
    }

    @Test
    void erroDoSearchServicePropagaParaOSpringAiTratar() {
        search.expect(requestTo("http://search/api/v1/livro/buscar?texto=x")).andRespond(withServerError());

        assertThatThrownBy(() -> tools.buscarLivro("x")).isInstanceOf(HttpServerErrorException.class);
    }

    // ---------- buscarLivroPublico ----------

    @Test
    void buscarLivroPublicoUsaARotaDeLivroPublicoEFormataOResultado() {
        esperaGet("http://search/api/v1/livro-publico/buscar?texto=dom",
                "[{\"titulo\":\"Dom Casmurro\",\"autor\":\"Machado de Assis\",\"descricao\":\"Romance\","
                        + "\"cursos\":[{\"id\":\"1\",\"nome\":\"Letras\"}],\"temas\":[]}]");

        String resultado = tools.buscarLivroPublico("dom");

        assertThat(resultado).contains("Título: Dom Casmurro", "Autor: Machado de Assis", "Cursos: Letras");
    }

    @Test
    void buscarLivroPublicoSemResultadoUsaAMesmaMensagemDeAcervo() {
        esperaGet("http://search/api/v1/livro-publico/buscar?texto=zzz", "[]");

        assertThat(tools.buscarLivroPublico("zzz")).isEqualTo("Nenhum livro encontrado no acervo para: zzz");
    }

    // ---------- buscarTcc ----------

    @Test
    void buscarTccMostraOCursoNoSingularEOsTemas() {
        esperaGet("http://search/api/v1/tcc/buscar?texto=redes",
                "[{\"titulo\":\"Redes Neurais\",\"autor\":\"Joao\",\"descricao\":\"TCC\","
                        + "\"curso\":{\"id\":\"1\",\"nome\":\"Computação\"},\"temas\":[{\"id\":\"2\",\"nome\":\"IA\"}]}]");

        String resultado = tools.buscarTcc("redes");

        assertThat(resultado).contains("Título: Redes Neurais", "Curso: Computação", "Temas: IA");
    }

    @Test
    void buscarTccSemCursoOuTemasUsaTextosPadrao() {
        esperaGet("http://search/api/v1/tcc/buscar?texto=x", "[{\"titulo\":\"Sem nada\",\"autor\":\"Fulano\"}]");

        assertThat(tools.buscarTcc("x")).contains("Curso: Curso não informado", "Temas: Nenhum tema vinculado", "Descrição: Sem descrição");
    }

    @Test
    void buscarTccSemResultadoDizQueNaoAchouTcc() {
        esperaGet("http://search/api/v1/tcc/buscar?texto=x", "[]");

        assertThat(tools.buscarTcc("x")).isEqualTo("Nenhum TCC encontrado no acervo para: x");
    }

    // ---------- buscarMaterialEstudo ----------

    @Test
    void buscarMaterialEstudoUsaARotaDeMaterialEFormataOResultado() {
        esperaGet("http://search/api/v1/material-estudo/buscar?texto=calculo",
                "[{\"titulo\":\"Lista de Cálculo\",\"autor\":\"Prof. Ana\",\"descricao\":\"Exercícios\","
                        + "\"cursos\":[{\"id\":\"1\",\"nome\":\"Engenharia\"}],\"temas\":[{\"id\":\"2\",\"nome\":\"Derivadas\"}]}]");

        String resultado = tools.buscarMaterialEstudo("calculo");

        assertThat(resultado).contains("Título: Lista de Cálculo", "Autor: Prof. Ana", "Cursos: Engenharia", "Temas: Derivadas");
    }

    @Test
    void buscarMaterialEstudoSemResultadoDizQueNaoAchouMaterial() {
        esperaGet("http://search/api/v1/material-estudo/buscar?texto=x", "[]");

        assertThat(tools.buscarMaterialEstudo("x")).isEqualTo("Nenhum material de estudo encontrado no acervo para: x");
    }

    @Test
    void camposDesconhecidosNaRespostaDoSearchServiceNaoQuebramODesserializador() {
        esperaGet("http://search/api/v1/livro/buscar?texto=x",
                "[{\"titulo\":\"A\",\"autor\":\"B\",\"id\":\"uuid\",\"quantidade\":3,\"dataPublicacao\":\"2020-01-01T00:00:00\"}]");

        assertThat(tools.buscarLivro("x")).contains("Título: A");
    }
}
