package com.digital.biblioteca;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Autenticacao, autorizacao por papel, cabecalhos de seguranca e tratamento de erros da API. */
class ApiSegurancaIT extends IntegracaoBase {

    static final String HEADER = "X-User-Matricula";

    @Autowired ObjectMapper objectMapper;

    private static String usuarioJson(String matricula, String senha, String role) {
        return """
                {"nome":"Maria Souza","matricula":"%s","senha":"%s","email":"%s@teste.com",
                 "telefone":"83999998888","role":"%s"}""".formatted(matricula, senha, matricula, role);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String corpo) {
        return builder.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    // ---------- cadastro de usuario (somente bibliotecario) ----------

    private MockHttpServletRequestBuilder comoBibliotecario(MockHttpServletRequestBuilder builder) {
        if (!usuarioRepository.existsByMatricula("BIB0001")) {
            criarUsuario("BIB0001", Role.BIBLIOTECARIO);
        }
        return builder.header(HEADER, "BIB0001");
    }

    @Test
    void bibliotecarioCadastraUsuarioValidoRetorna201EGravaASenhaHasheada() throws Exception {
        mvc.perform(json(comoBibliotecario(post("/api/v1/usuario")), usuarioJson("2023001", "Senha1234", "ALUNO")))
                .andExpect(status().isCreated());

        Usuario salvo = usuarioRepository.findByMatricula("2023001").orElseThrow();
        assertThat(salvo.getSenha()).startsWith("$2a$").isNotEqualTo("Senha1234");
        assertThat(passwordEncoder.matches("Senha1234", salvo.getSenha())).isTrue();
        assertThat(salvo.getRole()).isEqualTo(Role.ALUNO);
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    void bibliotecarioPodeCriarOutroBibliotecario() throws Exception {
        mvc.perform(json(comoBibliotecario(post("/api/v1/usuario")), usuarioJson("2023999", "Senha1234", "BIBLIOTECARIO")))
                .andExpect(status().isCreated());

        assertThat(usuarioRepository.findByMatricula("2023999").orElseThrow().getRole()).isEqualTo(Role.BIBLIOTECARIO);
    }

    @Test
    void cadastroSemLoginEBarradoEnaoCriaNada() throws Exception {
        // No core, anonimo recebe 403; o 401 de verdade vem do gateway (coberto em JwtAuthenticationFilterTest).
        mvc.perform(json(post("/api/v1/usuario"), usuarioJson("2023999", "Senha1234", "BIBLIOTECARIO")))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.existsByMatricula("2023999")).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"ALUNO", "PROFESSOR"})
    void alunoEProfessorNaoCadastramUsuariosNemElevandoOProprioPapel(String papel) throws Exception {
        criarUsuario("COMUM01", Role.valueOf(papel));

        mvc.perform(json(post("/api/v1/usuario").header(HEADER, "COMUM01"), usuarioJson("2023999", "Senha1234", "BIBLIOTECARIO")))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.existsByMatricula("2023999")).isFalse();
    }

    @Test
    void cadastroComSenhaFracaRetorna400ListandoOsCamposInvalidos() throws Exception {
        MvcResult resultado = mvc.perform(json(comoBibliotecario(post("/api/v1/usuario")), usuarioJson("2023001", "fraca", "ALUNO")))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode erro = corpo(resultado);
        assertThat(erro.get("message").asText()).isEqualTo("Erro de validação");
        assertThat(erro.get("details").asText()).contains("senha:");
        assertThat(erro.get("path").asText()).isEqualTo("/api/v1/usuario");
        assertThat(usuarioRepository.existsByMatricula("2023001")).isFalse();
    }

    @Test
    void cadastroDeMatriculaOuEmailJaExistenteRetorna422() throws Exception {
        criarUsuario("2023001", Role.ALUNO);

        mvc.perform(json(comoBibliotecario(post("/api/v1/usuario")), usuarioJson("2023001", "Senha1234", "ALUNO")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cadastroComRoleForaDaListaRetorna400() throws Exception {
        mvc.perform(json(comoBibliotecario(post("/api/v1/usuario")), usuarioJson("2023001", "Senha1234", "DIRETOR")))
                .andExpect(status().isBadRequest());
    }

    // ---------- login ----------

    @Test
    void loginValidoRetorna200ComTokensNoCorpoEDoisCookiesHttpOnly() throws Exception {
        criarUsuario("2023001", Role.ALUNO);

        MvcResult resultado = mvc.perform(json(post("/api/v1/auth/login"), "{\"matricula\":\"2023001\",\"senha\":\"Senha1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode corpo = corpo(resultado);
        assertThat(corpo.get("tokenAcesso").asText()).isNotBlank();
        assertThat(corpo.get("tokenRefresh").asText()).isNotBlank();
        List<String> cookies = resultado.getResponse().getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0)).startsWith("smartteca_access=").contains("HttpOnly", "Path=/");
        assertThat(cookies.get(1)).startsWith("smartteca_refresh=").contains("HttpOnly", "Path=/api/v1/auth");
    }

    @ParameterizedTest
    @CsvSource({"2023001,SenhaErrada1", "9999999,Senha1234"})
    void loginComSenhaErradaOuUsuarioInexistenteRetornaOMesmo401(String matricula, String senha) throws Exception {
        criarUsuario("2023001", Role.ALUNO);

        MvcResult resultado = mvc.perform(json(post("/api/v1/auth/login"),
                        "{\"matricula\":\"%s\",\"senha\":\"%s\"}".formatted(matricula, senha)))
                .andExpect(status().isUnauthorized())
                .andReturn();

        JsonNode erro = corpo(resultado);
        assertThat(erro.get("message").asText()).isEqualTo("Credenciais inválidas");
        assertThat(erro.get("details").asText()).isEqualTo("Matrícula ou senha incorretas.");
        assertThat(resultado.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    void loginComPayloadInvalidoRetorna400() throws Exception {
        mvc.perform(json(post("/api/v1/auth/login"), "{\"matricula\":\"12\",\"senha\":\"curta\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- conta desativada ----------

    @Test
    void contaDesativadaNaoLogaEARespostaEIgualADeSenhaErrada() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        jdbc.update("UPDATE usuario SET ativo = false WHERE matricula = ?", "2023001");

        MvcResult resultado = mvc.perform(json(post("/api/v1/auth/login"), "{\"matricula\":\"2023001\",\"senha\":\"Senha1234\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(corpo(resultado).get("message").asText()).isEqualTo("Credenciais inválidas");
        assertThat(resultado.getResponse().getHeaders("Set-Cookie")).isEmpty();
    }

    @Test
    void desativarAContaCortaOAcessoEORefreshDeQuemJaTinhaTokens() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        MvcResult login = mvc.perform(json(post("/api/v1/auth/login"), "{\"matricula\":\"2023001\",\"senha\":\"Senha1234\"}")).andReturn();
        String refresh = corpo(login).get("tokenRefresh").asText();
        mvc.perform(get("/api/v1/usuario/me").header(HEADER, "2023001")).andExpect(status().isOk());

        jdbc.update("UPDATE usuario SET ativo = false WHERE matricula = ?", "2023001");

        mvc.perform(get("/api/v1/usuario/me").header(HEADER, "2023001")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("smartteca_refresh", refresh))).andExpect(status().isNotFound());
    }

    // ---------- refresh e logout ----------

    @Test
    void refreshComCookieValidoEmiteNovosTokens() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        MvcResult login = mvc.perform(json(post("/api/v1/auth/login"), "{\"matricula\":\"2023001\",\"senha\":\"Senha1234\"}")).andReturn();
        String refresh = corpo(login).get("tokenRefresh").asText();

        MvcResult resultado = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("smartteca_refresh", refresh)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(corpo(resultado).get("tokenAcesso").asText()).isNotBlank();
        assertThat(resultado.getResponse().getHeaders("Set-Cookie")).hasSize(2);
    }

    @Test
    void refreshSemCookieRetorna422() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnprocessableEntity()).andReturn();

        assertThat(corpo(resultado).get("details").asText()).isEqualTo("Refresh token ausente.");
    }

    @Test
    void refreshComAccessTokenNoLugarDoRefreshNaoEhAceito() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        MvcResult login = mvc.perform(json(post("/api/v1/auth/login"), "{\"matricula\":\"2023001\",\"senha\":\"Senha1234\"}")).andReturn();
        String access = corpo(login).get("tokenAcesso").asText();

        MvcResult resultado = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("smartteca_refresh", access))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void refreshComTokenLixoRetorna401EnaoVazaDetalheInternoDaBiblioteca() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("smartteca_refresh", "lixo.lixo.lixo"))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("JWTDecodeException", "com.auth0");
    }

    @Test
    void refreshDeUsuarioRemovidoRetorna404() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        MvcResult login = mvc.perform(json(post("/api/v1/auth/login"), "{\"matricula\":\"2023001\",\"senha\":\"Senha1234\"}")).andReturn();
        String refresh = corpo(login).get("tokenRefresh").asText();
        jdbc.update("DELETE FROM usuario");

        mvc.perform(post("/api/v1/auth/refresh").cookie(new Cookie("smartteca_refresh", refresh)))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutRetorna204EExpiraOsDoisCookies() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent()).andReturn();

        List<String> cookies = resultado.getResponse().getHeaders("Set-Cookie");
        assertThat(cookies).hasSize(2).allSatisfy(c -> assertThat(c).contains("Max-Age=0"));
    }

    // ---------- /usuario/me e filtro do gateway ----------

    @Test
    void meDevolveOUsuarioDoHeaderDoGatewaySemExporASenha() throws Exception {
        criarUsuario("2023001", Role.PROFESSOR);

        MvcResult resultado = mvc.perform(get("/api/v1/usuario/me").header(HEADER, "2023001"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode corpo = corpo(resultado);
        assertThat(corpo.get("matricula").asText()).isEqualTo("2023001");
        assertThat(corpo.get("role").asText()).isEqualTo("PROFESSOR");
        assertThat(corpo.has("senha")).isFalse();
        assertThat(corpo.has("password")).isFalse();
    }

    @Test
    void rotaProtegidaSemHeaderDoGatewayNaoEhAcessivel() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/v1/usuario/me")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isIn(401, 403);
    }

    @Test
    void headerComMatriculaQueNaoExisteRetorna401EmJson() throws Exception {
        MvcResult resultado = mvc.perform(get("/api/v1/usuario/me").header(HEADER, "9999999"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(corpo(resultado).get("message").asText()).isEqualTo("Usuário do header não existe mais");
    }

    // ---------- autorizacao por papel ----------

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            POST   | /api/v1/curso/cadastrar | {"nome":"Direito"}
            PUT    | /api/v1/curso/atualizar | {"cursoId":"00000000-0000-0000-0000-000000000001","novoCurso":"Novo"}
            DELETE | /api/v1/curso/deletar/00000000-0000-0000-0000-000000000001 | {}
            POST   | /api/v1/tema/cadastrar | {"nome":"Algoritmos"}
            PUT    | /api/v1/tema/atualizar | {"temaId":"00000000-0000-0000-0000-000000000001","novoTema":"Novo"}
            DELETE | /api/v1/tema/deletar/00000000-0000-0000-0000-000000000001 | {}
            DELETE | /api/v1/livro/deletar/00000000-0000-0000-0000-000000000001 | {}
            DELETE | /api/v1/tcc/deletar/00000000-0000-0000-0000-000000000001 | {}
            DELETE | /api/v1/livro-publico/deletar/00000000-0000-0000-0000-000000000001 | {}
            POST   | /api/v1/reserva/reservar | {"usuario":"2023001","livro":"00000000-0000-0000-0000-000000000001"}
            PUT    | /api/v1/reserva/devolver | {"reserva":"00000000-0000-0000-0000-000000000001"}
            """)
    void rotasDeGestaoSaoNegadasParaAlunoEProfessor(String metodo, String url, String corpo) throws Exception {
        criarUsuario("ALUNO01", Role.ALUNO);
        criarUsuario("PROF001", Role.PROFESSOR);

        for (String matricula : List.of("ALUNO01", "PROF001")) {
            MockHttpServletRequestBuilder requisicao = switch (metodo) {
                case "POST" -> post(url);
                case "PUT" -> put(url);
                default -> delete(url);
            };
            mvc.perform(requisicao.header(HEADER, matricula).contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void alunoNaoPodeCadastrarMaterialDeEstudoMasProfessorEBibliotecarioPodemPassarDaAutorizacao() throws Exception {
        criarUsuario("ALUNO01", Role.ALUNO);
        criarUsuario("PROF001", Role.PROFESSOR);
        criarUsuario("BIB0001", Role.BIBLIOTECARIO);

        mvc.perform(delete("/api/v1/material-estudo/deletar/00000000-0000-0000-0000-000000000001").header(HEADER, "ALUNO01"))
                .andExpect(status().isForbidden());
        // professor e bibliotecario passam da autorizacao e chegam no service (404: material nao existe)
        mvc.perform(delete("/api/v1/material-estudo/deletar/00000000-0000-0000-0000-000000000001").header(HEADER, "PROF001"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/material-estudo/deletar/00000000-0000-0000-0000-000000000001").header(HEADER, "BIB0001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rotaNaoMapeadaNaoEAcessivelNemParaBibliotecario() throws Exception {
        criarUsuario("BIB0001", Role.BIBLIOTECARIO);

        MvcResult resultado = mvc.perform(get("/api/v1/inexistente").header(HEADER, "BIB0001")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isIn(401, 403, 404);
        assertThat(resultado.getResponse().getStatus()).isNotEqualTo(200);
    }

    // ---------- rotas publicas de infraestrutura ----------

    @Test
    void documentacaoOpenApiEPublicaEDescreveASmartTeca() throws Exception {
        MvcResult resultado = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();

        assertThat(corpo(resultado).get("info").get("title").asText()).isEqualTo("SmartTeca API");
    }

    @Test
    void healthEPublico() throws Exception {
        int status = mvc.perform(get("/actuator/health")).andReturn().getResponse().getStatus();

        assertThat(status).isIn(200, 503);
    }

    // ---------- cabecalhos de seguranca e correlation id ----------

    @Test
    void respostasTrazemOsCabecalhosDeSegurancaConfigurados() throws Exception {
        MvcResult resultado = mvc.perform(post("/api/v1/auth/logout").secure(true)).andReturn();

        var response = resultado.getResponse();
        assertThat(response.getHeader("X-Frame-Options")).isEqualTo("DENY");
        assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeader("Content-Security-Policy")).isEqualTo("default-src 'none'; frame-ancestors 'none'");
        assertThat(response.getHeader("Strict-Transport-Security")).contains("max-age=31536000").contains("includeSubDomains");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("strict-origin-when-cross-origin");
        assertThat(response.getHeader("Permissions-Policy")).contains("geolocation=()");
    }

    @Test
    void correlationIdDoGatewayEEcoadoEUmNovoEGeradoQuandoAusente() throws Exception {
        var comId = mvc.perform(post("/api/v1/auth/logout").header("X-Correlation-Id", "corr-123")).andReturn().getResponse();
        var semId = mvc.perform(post("/api/v1/auth/logout")).andReturn().getResponse();

        assertThat(comId.getHeader("X-Correlation-Id")).isEqualTo("corr-123");
        assertThat(semId.getHeader("X-Correlation-Id")).isNotBlank();
    }

    // ---------- tratamento de erros ----------

    @Test
    void jsonMalformadoRetorna400EnaoVaza500() throws Exception {
        MvcResult resultado = mvc.perform(json(post("/api/v1/auth/login"), "{matricula: sem aspas")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void uuidInvalidoNoCaminhoRetorna400() throws Exception {
        criarUsuario("BIB0001", Role.BIBLIOTECARIO);

        MvcResult resultado = mvc.perform(delete("/api/v1/curso/deletar/nao-e-uuid").header(HEADER, "BIB0001")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void metodoHttpNaoSuportadoRetorna405() throws Exception {
        criarUsuario("BIB0001", Role.BIBLIOTECARIO);

        MvcResult resultado = mvc.perform(get("/api/v1/curso/cadastrar").header(HEADER, "BIB0001")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(405);
    }

    @Test
    void errosNaoVazamStackTraceNemNomesDeClasseInternas() throws Exception {
        MvcResult resultado = mvc.perform(json(post("/api/v1/auth/login"), "{matricula: sem aspas")).andReturn();

        assertThat(resultado.getResponse().getContentAsString()).doesNotContain("at com.", "java.lang.", "org.springframework");
    }
}
