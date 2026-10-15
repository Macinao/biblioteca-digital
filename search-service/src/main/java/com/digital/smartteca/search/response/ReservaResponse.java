package com.digital.smartteca.search.response;

import com.digital.smartteca.common.enums.StatusReserva;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
public class ReservaResponse {

    UUID id;

    UUID livroId;

    String tituloLivro;

    String autorLivro;

    String matriculaUsuario;

    String nomeUsuario;

    StatusReserva statusReserva;

    LocalDateTime dataCriacao;

    LocalDateTime dataDevolucao;

}
