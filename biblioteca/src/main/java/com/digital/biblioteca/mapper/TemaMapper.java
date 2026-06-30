package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.TemaDocument;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.request.CadastrarTemaRequest;
import com.digital.biblioteca.response.TemaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import java.util.List;

@Mapper(componentModel = "spring", uses = ComumMapper.class)
public interface TemaMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true)
    })
    Tema cadastroRequestParaTema(CadastrarTemaRequest request);

    TemaDocument temaParaDocument(Tema tema);

    TemaResponse temaParaResponse(Tema tema);

    List<TemaResponse> listaDocumentParaResponse(Iterable<TemaDocument> documents);

}
