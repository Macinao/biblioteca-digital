package com.digital.biblioteca.request;

import jakarta.validation.constraints.Min;
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
public class AtualizarLivroRequest {

    @NotNull
    UUID id;

    @NotBlank
    String titulo;

    @NotBlank
    String autor;

    @NotBlank
    String descricao;

    @NotEmpty
    List<@NotNull UUID> cursos;

    @NotEmpty
    List<@NotNull UUID> temas;

    @NotNull
    LocalDateTime dataPublicacao;

    @NotNull
    @Min(0)
    Integer quantidade;

}
