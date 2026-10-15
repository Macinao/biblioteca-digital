package com.digital.biblioteca.response;

import com.digital.smartteca.common.enums.StatusReserva;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
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
