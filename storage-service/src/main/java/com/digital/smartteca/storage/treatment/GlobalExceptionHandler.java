package com.digital.smartteca.storage.treatment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

/**
 * @RestControllerAdvice funciona igual em WebFlux — so que o metodo
 * recebe um ServerWebExchange (nao HttpServletRequest) pra pegar o path.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNotFound(RecursoNaoEncontradoException ex, ServerWebExchange exchange) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(HttpStatus.NOT_FOUND, "Recurso não encontrado",
                        ex.getMessage(), exchange.getRequest().getPath().value()));

    }

    @ExceptionHandler(IntegracaoExternaException.class)
    public ResponseEntity<ApiError> handleIntegracaoExterna(IntegracaoExternaException ex, ServerWebExchange exchange) {

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiError.of(HttpStatus.BAD_GATEWAY, "Falha em sistema externo",
                        ex.getMessage(), exchange.getRequest().getPath().value()));

    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, ServerWebExchange exchange) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(HttpStatus.FORBIDDEN, "Acesso negado",
                        ex.getMessage(), exchange.getRequest().getPath().value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, ServerWebExchange exchange) {

        log.error("Erro interno na request {} {}: ", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor",
                        ex.getClass().getSimpleName() + ": " + ex.getMessage(),
                        exchange.getRequest().getPath().value()));

    }

}
