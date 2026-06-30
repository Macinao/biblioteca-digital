package com.digital.biblioteca.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CadastrarCursoRequest {

    @NotBlank
    String nome;

}
