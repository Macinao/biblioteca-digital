package com.digital.smartteca.search.treatment;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sem este handler, uma falha interna (ex.: Elasticsearch fora do ar) cai no redirecionamento para /error,
 * que a SecurityConfig bloqueia (anyRequest().denyAll()): o cliente recebia "403 com corpo vazio" em vez
 * de um erro que dê para entender e monitorar.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ApiError> handleNaoEncontrado(NaoEncontradoException ex, HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ApiError> handleRequisicaoInvalida(RequisicaoInvalidaException ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage(), request);
    }

    // Parâmetro com tipo errado (?pagina=abc) ou corpo ilegível: são RuntimeException no Spring, então precisam
    // de handler próprio, senão o catch-all abaixo os devolveria como 500.
    @ExceptionHandler({TypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleMalformada(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", "Verifique os parâmetros enviados.", request);
    }

    // @PreAuthorize lança dentro do controller: sem este handler o catch-all abaixo o transformaria em 500.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para este recurso.", request);
    }

    // Parâmetro obrigatório ausente, rota inexistente e método errado são ServletException, não RuntimeException:
    // continuam com o tratamento padrão do Spring (400/404/405) e não passam por aqui.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleInterno(RuntimeException ex, HttpServletRequest request) {
        log.error("Erro interno em {}", request.getRequestURI(), ex);
        // A mensagem real fica só no log: não vaza detalhe de infraestrutura para o cliente.
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", "Tente novamente em instantes.", request);
    }

    private static ResponseEntity<ApiError> resposta(HttpStatus status, String mensagem, String detalhes, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, mensagem, detalhes, request.getRequestURI()));
    }
}
