package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.MaterialEstudoDocument;
import com.digital.biblioteca.model.MaterialEstudo;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.response.MaterialEstudoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(
        componentModel = "spring",
        uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class}
)
public interface MaterialEstudoMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "cursos", ignore = true),
            @Mapping(target = "conteudo", ignore = true),
            @Mapping(target = "criador", ignore = true)
    })
    MaterialEstudo cadastroRequestParaMaterialEstudo(CadastrarMaterialEstudoRequest request);

    MaterialEstudoDocument materialEstudoParaDocument(MaterialEstudo materialEstudo);

    MaterialEstudoResponse materialEstudoParaResponse(MaterialEstudo materialEstudo);

    @Mappings({
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "cursos", ignore = true),
            @Mapping(target = "conteudo", ignore = true),
            @Mapping(target = "criador", ignore = true)
    })
    void atualizarRequestParaMaterialEstudo(
            AtualizarMaterialEstudoRequest request,
            @MappingTarget MaterialEstudo materialEstudo
    );

}
