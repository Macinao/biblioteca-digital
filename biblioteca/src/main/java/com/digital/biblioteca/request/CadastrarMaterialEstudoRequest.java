package com.digital.biblioteca.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class CadastrarMaterialEstudoRequest {

    @NotBlank
    String titulo;

    @NotBlank
    String autor;

    @NotBlank
    String descricao;

    @NotEmpty
    List<@NotNull UUID> temas;

    @NotEmpty
    List<@NotNull UUID> cursos;

    @NotNull
    LocalDateTime dataPublicacao;

}
