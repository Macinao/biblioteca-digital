package com.digital.biblioteca.document;

import com.digital.biblioteca.config.ai.Mensagem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "memoria")
public class MemoriaDocument {

    @Id
    private String id;

    private List<Mensagem> conversas = new ArrayList<>();

}
