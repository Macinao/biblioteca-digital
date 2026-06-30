package com.digital.biblioteca.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EnviarMensagemChatbotRequest {

    @NotBlank
    @Size(min = 1, max = 2000)
    private String mensagem;

}
