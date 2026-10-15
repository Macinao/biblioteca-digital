package com.digital.biblioteca.document;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * So um DTO — o formato do payload que vai pro Kafka via outbox. Ver
 * comentario em CursoDocument.
 */
@Setter
@Getter
public class LivroPublicoDocument {

    private String id;

    private String titulo;

    private String autor;

    private String descricao;

    private List<CursoDocument> cursos;

    private List<TemaDocument> temas;

    private LocalDateTime dataPublicacao;

    private String capa;

    private String conteudo;

}
