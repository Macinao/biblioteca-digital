package com.digital.biblioteca.controller;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.EnviarMensagemChatbotRequest;
import com.digital.biblioteca.service.implement.ChatbotServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotServiceImpl chatbotService;

    @PostMapping("/mensagem")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<String> enviarMensagem(
            @Valid @RequestBody EnviarMensagemChatbotRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(chatbotService.chatbotConversa(request.getMensagem(), usuario));

    }

}
