package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.LivroPublicoDocument;
import com.digital.biblioteca.model.LivroPublico;
import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.response.LivroPublicoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(
        componentModel = "spring",
        uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class}
)
public interface LivroPublicoMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "cursos", ignore = true),
            @Mapping(target = "capa", ignore = true),
            @Mapping(target = "conteudo", ignore = true)
    })
    LivroPublico cadastroRequestParaLivroPublico(CadastrarLivroPublicoRequest request);

    LivroPublicoDocument livroPublicoParaDocument(LivroPublico livroPublico);

    LivroPublicoResponse livroPublicoParaResponse(LivroPublico livroPublico);

    @Mappings({
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "temas", ignore = true),
            @Mapping(target = "cursos", ignore = true),
            @Mapping(target = "capa", ignore = true),
            @Mapping(target = "conteudo", ignore = true)
    })
    void atualizarRequestParaLivroPublico(
            AtualizarLivroPublicoRequest request,
            @MappingTarget LivroPublico livro
    );

}
