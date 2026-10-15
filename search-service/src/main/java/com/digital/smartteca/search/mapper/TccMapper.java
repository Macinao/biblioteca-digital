package com.digital.smartteca.search.mapper;

import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.response.TccResponse;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class})
public interface TccMapper {

    TccResponse documentParaResponse(TccDocument document);

    List<TccResponse> listaDocumentParaResponse(Iterable<TccDocument> documents);

}
