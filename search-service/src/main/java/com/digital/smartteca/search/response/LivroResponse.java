package com.digital.smartteca.search.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class LivroResponse {

    UUID id;

    String titulo;

    String autor;

    String descricao;

    List<CursoResponse> cursos;

    List<TemaResponse> temas;

    LocalDateTime dataPublicacao;

    Integer quantidade;

}
