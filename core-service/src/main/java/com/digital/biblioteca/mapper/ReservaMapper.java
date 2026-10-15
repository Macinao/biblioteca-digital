package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.ReservaDocument;
import com.digital.biblioteca.model.Reserva;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(
        componentModel = "spring",
        uses = {ComumMapper.class}
)
public interface ReservaMapper {

    @Mappings({
            @Mapping(target = "livroId", source = "livro.id"),
            @Mapping(target = "tituloLivro", source = "livro.titulo"),
            @Mapping(target = "autorLivro", source = "livro.autor"),
            @Mapping(target = "matriculaUsuario", source = "usuario.matricula"),
            @Mapping(target = "nomeUsuario", source = "usuario.nome")
    })
    ReservaDocument reservaParaDocument(Reserva reserva);

}
