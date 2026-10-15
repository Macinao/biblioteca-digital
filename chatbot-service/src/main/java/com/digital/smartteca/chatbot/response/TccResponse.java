package com.digital.smartteca.chatbot.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TccResponse {

    String titulo;
    String autor;
    String descricao;
    CursoResponse curso;
    List<TemaResponse> temas;

}
