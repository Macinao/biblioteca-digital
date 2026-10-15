package com.digital.biblioteca.document;

import lombok.Getter;
import lombok.Setter;

/**
 * So um DTO — o formato do payload que vai pro Kafka via outbox. Ver
 * comentario em CursoDocument.
 */
@Setter
@Getter
public class TemaDocument {

    private String id;

    private String nome;

}
