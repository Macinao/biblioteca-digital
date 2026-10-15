package com.digital.smartteca.chatbot.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LivroResponse {

    String titulo;
    String autor;
    String descricao;
    List<CursoResponse> cursos;
    List<TemaResponse> temas;

}
