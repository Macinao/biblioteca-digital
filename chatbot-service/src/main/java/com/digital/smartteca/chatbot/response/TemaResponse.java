package com.digital.smartteca.chatbot.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemaResponse {

    String id;
    String nome;

}
