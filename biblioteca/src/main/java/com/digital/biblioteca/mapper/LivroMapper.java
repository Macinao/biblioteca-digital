package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.LivroDocument;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.request.AtualizarLivroRequest;
import com.digital.biblioteca.request.CadastrarLivroRequest;
import com.digital.biblioteca.response.LivroResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class}
)
public interface LivroMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "cursos", ignore = true),
            @Mapping(target = "capa", ignore = true)
    })
    Livro cadastroRequestParaLivro(CadastrarLivroRequest request);

    LivroDocument livroParaDocument(Livro livro);

    LivroResponse livroParaResponse(Livro livro);

    LivroResponse documentParaResponse(LivroDocument document);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "cursos", ignore = true),
            @Mapping(target = "capa", ignore = true)
    })
    void atualizarRequestParaLivro(
            AtualizarLivroRequest request,
            @MappingTarget Livro livro
    );

    List<LivroResponse> listaDocumentParaResponse(Iterable<LivroDocument> documents);

}
