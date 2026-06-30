package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.TccDocument;
import com.digital.biblioteca.model.Tcc;
import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.response.TccResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class}
)
public interface TccMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "curso", ignore = true),
            @Mapping(target = "conteudo", ignore = true)
    })
    Tcc cadastroRequestParaTcc(CadastrarTccRequest request);

    TccDocument tccParaDocument(Tcc tcc);

    TccResponse tccParaResponse(Tcc tcc);

    @Mappings({
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "curso", ignore = true),
            @Mapping(target = "conteudo", ignore = true)
    })
    void atualizarRequestParaTcc(
            AtualizarTccRequest request,
            @MappingTarget Tcc tcc
    );

    List<TccResponse> listaDocumentParaResponse(Iterable<TccDocument> documents);

    List<TccDocument> listaDocument(Iterable<TccDocument> documents);

}
