package com.digital.biblioteca.document;

import com.digital.biblioteca.enums.StatusReserva;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.LocalDateTime;

@Getter
@Setter
@Document(indexName = "reserva")
public class ReservaDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String livroId;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String tituloLivro;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String autorLivro;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String matriculaUsuario;

    @Field(
            type = FieldType.Text,
            analyzer = "brazilian"
    )
    private String nomeUsuario;

    @Field(type = FieldType.Keyword)
    private StatusReserva statusReserva;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dataCriacao;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime dataDevolucao;

}
