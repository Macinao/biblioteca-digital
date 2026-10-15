package com.digital.biblioteca.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class LivroPublicoResponse {

    UUID id;

    String titulo;

    String autor;

    String descricao;

    List<TemaResponse> temas;

    List<CursoResponse> cursos;

    LocalDateTime dataPublicacao;

}
