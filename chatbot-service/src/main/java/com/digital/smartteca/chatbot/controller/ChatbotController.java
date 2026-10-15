package com.digital.smartteca.chatbot.controller;

import com.digital.smartteca.chatbot.request.EnviarMensagemChatbotRequest;
import com.digital.smartteca.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/mensagem")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<String> enviarMensagem(
            @Valid @RequestBody EnviarMensagemChatbotRequest request,
            Authentication authentication) {

        String matricula = authentication.getName();

        return ResponseEntity.status(HttpStatus.OK)
                .body(chatbotService.chatbotConversa(request.getMensagem(), matricula));

    }
}
