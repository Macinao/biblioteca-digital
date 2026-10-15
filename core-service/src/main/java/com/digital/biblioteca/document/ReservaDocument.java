package com.digital.biblioteca.document;

import com.digital.smartteca.common.enums.StatusReserva;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * So um DTO — o formato do payload que vai pro Kafka via outbox. Ver
 * comentario em CursoDocument.
 */
@Getter
@Setter
public class ReservaDocument {

    private String id;

    private String livroId;

    private String tituloLivro;

    private String autorLivro;

    private String matriculaUsuario;

    private String nomeUsuario;

    private StatusReserva statusReserva;

    private LocalDateTime dataCriacao;

    private LocalDateTime dataDevolucao;

}
