package com.digital.biblioteca.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Setter
@Getter
public class AtualizarTemaRequest {

    @NotNull
    UUID temaId;

    @NotBlank
    String novoTema;

}
