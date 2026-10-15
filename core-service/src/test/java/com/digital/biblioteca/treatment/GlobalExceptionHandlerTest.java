package com.digital.biblioteca.treatment;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/livro");

    private static void verifica(ResponseEntity<ApiError> resposta, HttpStatus status, String mensagem) {
        assertThat(resposta.getStatusCode()).isEqualTo(status);
        ApiError erro = resposta.getBody();
        assertThat(erro).isNotNull();
        assertThat(erro.getStatusCode()).isEqualTo(status.value());
        assertThat(erro.getStatus()).isEqualTo(status.name());
        assertThat(erro.getMessage()).isEqualTo(mensagem);
        assertThat(erro.getPath()).isEqualTo("/api/v1/livro");
        assertThat(erro.getTimestamp()).isNotNull();
    }

    @Test
    void recursoNaoEncontradoVira404ComAMensagemDaExcecaoNosDetalhes() {
        var resposta = handler.handleNotFound(new RecursoNaoEncontradoException("Livro 7 não existe"), request);

        verifica(resposta, HttpStatus.NOT_FOUND, "Recurso não encontrado");
        assertThat(resposta.getBody().getDetails()).isEqualTo("Livro 7 não existe");
    }

    @Test
    void entityNotFoundDoJpaTambemVira404() {
        var resposta = handler.handleNotFound(new EntityNotFoundException("sem linha"), request);

        verifica(resposta, HttpStatus.NOT_FOUND, "Recurso não encontrado");
    }

    @Test
    void regraDeNegocioVira422() {
        var resposta = handler.handleRegraNegocio(new RegraNegocioException("Livro sem exemplares"), request);

        verifica(resposta, HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada");
        assertThat(resposta.getBody().getDetails()).isEqualTo("Livro sem exemplares");
    }

    @Test
    void integracaoExternaVira502() {
        var resposta = handler.handleIntegracaoExterna(new IntegracaoExternaException("Supabase fora do ar"), request);

        verifica(resposta, HttpStatus.BAD_GATEWAY, "Falha em sistema externo");
    }

    @Test
    void credenciaisInvalidasViram401ComMensagemGenericaQueNaoRevelaQualCampoErrou() {
        var resposta = handler.handleBadCredentials(new BadCredentialsException("senha errada para 2023001"), request);

        verifica(resposta, HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        assertThat(resposta.getBody().getDetails()).isEqualTo("Matrícula ou senha incorretas.");
        assertThat(resposta.getBody().getDetails()).doesNotContain("2023001");
    }

    @Test
    void contaDesativadaNoLoginTemARespostaIdenticaADeSenhaErradaParaNaoRevelarQueAMatriculaExiste() {
        var resposta = handler.handleBadCredentials(new DisabledException("User is disabled"), request);

        verifica(resposta, HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        assertThat(resposta.getBody().getDetails()).isEqualTo("Matrícula ou senha incorretas.");
    }

    @Test
    void falhaDeAutenticacaoGenericaVira401() {
        var resposta = handler.handleAuth(new InsufficientAuthenticationException("sem credenciais"), request);

        verifica(resposta, HttpStatus.UNAUTHORIZED, "Falha de autenticação");
    }

    @Test
    void acessoNegadoVira403() {
        var resposta = handler.handleAccessDenied(new AccessDeniedException("Access Denied"), request);

        verifica(resposta, HttpStatus.FORBIDDEN, "Acesso negado");
    }

    @Test
    void erroDeValidacaoVira400ComTodosOsCamposInvalidosNosDetalhes() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.rejectValue(null, "x"); // erro global, nao entra nos detalhes de campo
        binding.addError(new org.springframework.validation.FieldError("request", "titulo", "não pode ser vazio"));
        binding.addError(new org.springframework.validation.FieldError("request", "quantidade", "deve ser positivo"));
        MethodParameter parametro = new MethodParameter(Object.class.getMethod("toString"), -1);

        var resposta = handler.handleValidation(new MethodArgumentNotValidException(parametro, binding), request);

        verifica(resposta, HttpStatus.BAD_REQUEST, "Erro de validação");
        assertThat(resposta.getBody().getDetails()).isEqualTo("titulo: não pode ser vazio, quantidade: deve ser positivo");
    }

    @Test
    void argumentoIlegalVira400() {
        var resposta = handler.handleIllegalArgument(new IllegalArgumentException("UUID inválido"), request);

        verifica(resposta, HttpStatus.BAD_REQUEST, "Argumento inválido");
        assertThat(resposta.getBody().getDetails()).isEqualTo("UUID inválido");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            ERROR: value too long for type character varying(255) | 400 | Texto muito longo
            Data too long for column 'titulo'                      | 400 | Texto muito longo
            null value in column "titulo" violates not-null constraint | 400 | Campo obrigatório ausente
            NOT NULL constraint failed                              | 400 | Campo obrigatório ausente
            duplicate key value violates unique constraint          | 409 | Conflito de dados
            Unique index or primary key violation                   | 409 | Conflito de dados
            Já existe um registro                                   | 409 | Conflito de dados
            violates foreign key constraint                         | 409 | Conflito de dados
            """)
    void violacaoDeIntegridadeEClassificadaPelaCausaRaiz(String causa, int statusEsperado, String titulo) {
        var excecao = new DataIntegrityViolationException("falha", new RuntimeException(causa));

        var resposta = handler.handleDataIntegrity(excecao, request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(statusEsperado);
        assertThat(resposta.getBody().getMessage()).isEqualTo(titulo);
    }

    @Test
    void violacaoDeUnicidadeTrazDetalheAmigavelSemVazarSqlDoBanco() {
        var excecao = new DataIntegrityViolationException("falha",
                new RuntimeException("duplicate key value violates unique constraint \"uk_usuario_matricula\""));

        var resposta = handler.handleDataIntegrity(excecao, request);

        assertThat(resposta.getBody().getDetails()).isEqualTo("Já existe um registro com esses dados.");
        assertThat(resposta.getBody().getDetails()).doesNotContain("uk_usuario_matricula");
    }

    @Test
    void violacaoComCausaSemMensagemCaiNoConflitoGenerico() {
        var excecao = new DataIntegrityViolationException("falha", new RuntimeException((String) null));

        var resposta = handler.handleDataIntegrity(excecao, request);

        verifica(resposta, HttpStatus.CONFLICT, "Conflito de dados");
        assertThat(resposta.getBody().getDetails()).isEqualTo("A operação viola uma restrição do banco de dados.");
    }

    @Test
    void rotaInexistenteVira404ComMetodoECaminhoNosDetalhes() {
        var resposta = handler.handleNoResourceFound(new NoResourceFoundException(HttpMethod.POST, "/api/v1/livro", "api/v1/livro"), request);

        verifica(resposta, HttpStatus.NOT_FOUND, "Rota não encontrada");
        assertThat(resposta.getBody().getDetails()).isEqualTo("Nenhum endpoint mapeado para POST /api/v1/livro");
    }

    @Test
    void partMultipartAusenteEMediaTypeNaoSuportadoViram400() {
        var ausente = handler.handleMultipart(new MissingServletRequestPartException("capa"), request);
        var tipo = handler.handleMultipart(new HttpMediaTypeNotSupportedException("text/plain"), request);

        verifica(ausente, HttpStatus.BAD_REQUEST, "Requisição inválida");
        verifica(tipo, HttpStatus.BAD_REQUEST, "Requisição inválida");
    }

    @Test
    void tokenJwtInvalidoVira401SemVazarOTipoDaExcecao() {
        var resposta = handler.handleTokenInvalido(
                new com.auth0.jwt.exceptions.JWTDecodeException("The token was expected to have 3 parts"), request);

        verifica(resposta, HttpStatus.UNAUTHORIZED, "Token inválido");
        assertThat(resposta.getBody().getDetails()).isEqualTo("Token inválido ou expirado.");
        assertThat(resposta.getBody().getDetails()).doesNotContain("JWTDecodeException", "3 parts");
    }

    @Test
    void jsonIlegivelEParametroComTipoErradoViram400() {
        var json = handler.handleRequisicaoMalformada(
                new org.springframework.http.converter.HttpMessageNotReadableException("JSON parse error", (org.springframework.http.HttpInputMessage) null), request);
        var tipo = handler.handleRequisicaoMalformada(new IllegalStateException("tipo"), request);

        verifica(json, HttpStatus.BAD_REQUEST, "Requisição malformada");
        verifica(tipo, HttpStatus.BAD_REQUEST, "Requisição malformada");
        assertThat(json.getBody().getDetails()).doesNotContain("JSON parse error");
    }

    @Test
    void metodoHttpNaoSuportadoVira405ComOMetodoNosDetalhes() {
        var resposta = handler.handleMetodoNaoSuportado(
                new org.springframework.web.HttpRequestMethodNotSupportedException("GET"), request);

        verifica(resposta, HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido");
        assertThat(resposta.getBody().getDetails()).isEqualTo("O método POST não é suportado nesta rota.");
    }

    @Test
    void excecaoNaoMapeadaVira500ComTipoEMensagemNosDetalhes() {
        var resposta = handler.handleGeneric(new IllegalStateException("estado quebrado"), request);

        verifica(resposta, HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor");
        assertThat(resposta.getBody().getDetails()).isEqualTo("IllegalStateException: estado quebrado");
    }
}
