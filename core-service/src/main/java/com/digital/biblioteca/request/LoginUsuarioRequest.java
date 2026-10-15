package com.digital.biblioteca.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginUsuarioRequest {

    @NotBlank
    @Size(min = 6, max = 20, message = "Matrícula deve ter entre 6 e 20 caracteres")
    String matricula;

    @NotBlank
    @Size(min = 8, max = 64, message = "Senha deve ter entre 8 e 64 caracteres")
    String senha;

}
