package com.digital.biblioteca.document;

import lombok.Getter;
import lombok.Setter;

/**
 * Apesar do nome (heranca do tempo em que core-service falava direto com
 * o Elasticsearch), isso hoje e so um DTO: o formato do payload que vai
 * pro Kafka via outbox. Quem realmente indexa isso no Elasticsearch e o
 * search-service, com sua propria copia anotada.
 */
@Setter
@Getter
public class CursoDocument {

    private String id;

    private String nome;

}
