package com.digital.biblioteca.treatment;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException ex,
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
