package com.digital.smartteca.search.mapper;

import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.response.MaterialEstudoResponse;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class})
public interface MaterialEstudoMapper {

    MaterialEstudoResponse documentParaResponse(MaterialEstudoDocument document);

    List<MaterialEstudoResponse> listaDocumentParaResponse(Iterable<MaterialEstudoDocument> documents);

}
