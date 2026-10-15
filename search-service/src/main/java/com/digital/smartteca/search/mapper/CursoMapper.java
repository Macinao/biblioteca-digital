package com.digital.smartteca.search.mapper;

import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.response.CursoResponse;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = ComumMapper.class)
public interface CursoMapper {

    CursoResponse documentParaResponse(CursoDocument document);

    List<CursoResponse> listaDocumentParaResponse(Iterable<CursoDocument> documents);

}
