package com.digital.biblioteca.treatment;

import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EntityNotFoundException.class, RecursoNaoEncontradoException.class})
    public ResponseEntity<ApiError> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        HttpStatus.NOT_FOUND,
                        "Recurso não encontrado",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiError> handleRegraNegocio(
            RegraNegocioException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Regra de negócio violada",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IntegracaoExternaException.class)
    public ResponseEntity<ApiError> handleIntegracaoExterna(
            IntegracaoExternaException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(
                        HttpStatus.BAD_GATEWAY,
                        "Falha em sistema externo",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // DisabledException junto: o Spring checa "conta ativa" antes da senha, então uma mensagem própria
    // revelaria a quem não sabe a senha que aquela matrícula existe e está desativada.
    @ExceptionHandler({BadCredentialsException.class, DisabledException.class})
    public ResponseEntity<ApiError> handleBadCredentials(
            AuthenticationException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        HttpStatus.UNAUTHORIZED,
                        "Credenciais inválidas",
                        "Matrícula ou senha incorretas.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(
            AuthenticationException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        HttpStatus.UNAUTHORIZED,
                        "Falha de autenticação",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(
                        HttpStatus.FORBIDDEN,
                        "Acesso negado",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST,
                        "Erro de validação",
                        details,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST,
                        "Argumento inválido",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        String causaRaw = ex.getMostSpecificCause().getMessage();
        String causa = causaRaw == null ? "" : causaRaw.toLowerCase();

        log.warn("Violação de integridade em {} {}: {}",
                request.getMethod(), request.getRequestURI(), causaRaw);

        HttpStatus status;
        String titulo;
        String detalhes;

        if (causa.contains("value too long") || causa.contains("data too long")) {
            status = HttpStatus.BAD_REQUEST;
            titulo = "Texto muito longo";
            detalhes = "Um dos campos enviados excede o tamanho máximo permitido.";
        } else if (causa.contains("null value") || causa.contains("not-null") || causa.contains("not null")) {
            status = HttpStatus.BAD_REQUEST;
            titulo = "Campo obrigatório ausente";
            detalhes = "Um campo obrigatório não foi preenchido.";
        } else if (causa.contains("duplicate") || causa.contains("unique") || causa.contains("já existe")) {
            status = HttpStatus.CONFLICT;
            titulo = "Conflito de dados";
            detalhes = "Já existe um registro com esses dados.";
        } else {
            status = HttpStatus.CONFLICT;
            titulo = "Conflito de dados";
            detalhes = "A operação viola uma restrição do banco de dados.";
        }

        return ResponseEntity
                .status(status)
                .body(ApiError.of(status, titulo, detalhes, request.getRequestURI()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(
                        HttpStatus.NOT_FOUND,
                        "Rota não encontrada",
                        "Nenhum endpoint mapeado para " + request.getMethod() + " " + request.getRequestURI(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler({MissingServletRequestPartException.class, HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiError> handleMultipart(
            Exception ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST,
                        "Requisição inválida",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    // Sem estes handlers, o catch-all abaixo transformava erro do cliente em 500 (e vazava o nome da classe).
    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ApiError> handleTokenInvalido(
            JWTVerificationException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(
                        HttpStatus.UNAUTHORIZED,
                        "Token inválido",
                        "Token inválido ou expirado.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleRequisicaoMalformada(
            Exception ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST,
                        "Requisição malformada",
                        "O corpo ou algum parâmetro da requisição tem formato inválido.",
                        request.getRequestURI()
                ));
    }

    // Endpoints com @Valid @RequestPart + @Imagem/@Pdf em parametros caem aqui (validacao de metodo), nao em MethodArgumentNotValidException.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleValidacaoDeParametros(
            HandlerMethodValidationException ex,
            HttpServletRequest request) {

        String details = ex.getParameterValidationResults().stream()
                .flatMap(resultado -> {
                    if (resultado instanceof ParameterErrors erros) {
                        return erros.getFieldErrors().stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage());
                    }
                    String parametro = resultado.getMethodParameter().getParameterName();
                    return resultado.getResolvableErrors().stream()
                            .map(e -> parametro + ": " + e.getDefaultMessage());
                })
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(
                        HttpStatus.BAD_REQUEST,
                        "Erro de validação",
                        details,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMetodoNaoSuportado(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "Método não permitido",
                        "O método " + request.getMethod() + " não é suportado nesta rota.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        log.error("Erro interno na request {} {}: ", request.getMethod(), request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erro interno do servidor",
                        ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        request.getRequestURI()
                ));
    }
}
