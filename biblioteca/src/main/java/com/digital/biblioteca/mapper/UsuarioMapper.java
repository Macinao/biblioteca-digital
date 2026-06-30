package com.digital.biblioteca.mapper;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.CadastrarUsuarioRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = ComumMapper.class)
public interface UsuarioMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "dataCriacao", ignore = true),
            @Mapping(target = "dataAtualizacao", ignore = true),
            @Mapping(target = "ativo", ignore = true),
            @Mapping(target = "authorities", ignore = true)
    })
    Usuario cadastroRequestParaUsuario(CadastrarUsuarioRequest request);

}
