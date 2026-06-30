package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.CursoDocument;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.request.CadastrarCursoRequest;
import com.digital.biblioteca.response.CursoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import java.util.List;

@Mapper(componentModel = "spring", uses = ComumMapper.class)
public interface CursoMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true)
    })
    Curso cadastroRequestParaCurso(CadastrarCursoRequest request);

    CursoDocument cursoParaDocument(Curso curso);

    CursoResponse cursoParaResponse(Curso curso);

    List<CursoResponse> listaDocumentParaResponse(Iterable<CursoDocument> documents);

}
