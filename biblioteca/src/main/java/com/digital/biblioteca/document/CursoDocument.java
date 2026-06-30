package com.digital.biblioteca.document;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Setter
@Getter
@Document(indexName = "curso")
public class CursoDocument {

    @Id
    private String id;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String nome;

}
