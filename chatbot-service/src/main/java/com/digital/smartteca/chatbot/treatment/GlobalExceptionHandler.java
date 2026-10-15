package com.digital.smartteca.chatbot.treatment;

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
 * Este serviço só depende de coisas externas (o modelo de IA e o Redis). Sem handler, quando uma delas falha a
 * exceção cai no /error, que a SecurityConfig bloqueia (anyRequest().denyAll()): o usuário via "403 vazio".
 * Agora ele recebe um 503 em JSON que o front sabe mostrar.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @PreAuthorize lança dentro do controller: sem este handler o catch-all abaixo o transformaria em 503.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado", "Você não tem permissão para este recurso.", request);
    }

    // Corpo ilegível (JSON quebrado) é RuntimeException no Spring: sem handler próprio viraria 503, e o erro seria do
    // cliente, não do assistente. (Validação de campos é BindException e segue com o tratamento padrão, 400.)
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<ApiError> handleMalformada(Exception ex, HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição inválida", "Verifique os dados enviados.", request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleIndisponivel(RuntimeException ex, HttpServletRequest request) {
        log.error("Falha ao atender o chatbot", ex);
        // A causa real (URL do modelo, stack) fica só no log.
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "Assistente indisponível",
                "O assistente não está disponível agora. Tente de novo em instantes.", request);
    }

    private static ResponseEntity<ApiError> resposta(HttpStatus status, String mensagem, String detalhes, HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, mensagem, detalhes, request.getRequestURI()));
    }
}
