package com.digital.biblioteca.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Document(indexName = "livro")
public class LivroDocument {

    @Id
    private String id;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String titulo;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String autor;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String descricao;

    @Field(
            type = FieldType.Nested
    )
    private List<TemaDocument> temas;

    @Field(
            type = FieldType.Nested
    )
    private List<CursoDocument> cursos;

    @Field(
            type = FieldType.Date,
            format = DateFormat.date_hour_minute_second
    )
    private LocalDateTime dataPublicacao;

    @Field(type = FieldType.Integer)
    private Integer quantidade;

}
