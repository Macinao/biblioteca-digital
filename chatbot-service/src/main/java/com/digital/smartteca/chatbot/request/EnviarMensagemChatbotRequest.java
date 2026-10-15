package com.digital.smartteca.chatbot.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnviarMensagemChatbotRequest {

    @NotBlank
    @Size(min = 1, max = 2000)
    private String mensagem;

}
