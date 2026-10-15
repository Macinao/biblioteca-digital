package com.digital.smartteca.search.response;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
public class TccResponse {

    UUID id;

    String titulo;

    String autor;

    String descricao;

    List<TemaResponse> temas;

    CursoResponse curso;

    LocalDateTime dataPublicacao;

}
