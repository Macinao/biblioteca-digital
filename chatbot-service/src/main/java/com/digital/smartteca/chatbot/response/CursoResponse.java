package com.digital.smartteca.chatbot.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO enxuto so com os campos que o MetodosChat realmente formata pro
 * LLM — o search-service devolve mais campos (id como UUID, etc.), por
 * isso ignoreUnknown=true em vez de espelhar a resposta inteira.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CursoResponse {

    String id;
    String nome;

}
