package com.digital.smartteca.search.mapper;

import com.digital.smartteca.search.document.TemaDocument;
import com.digital.smartteca.search.response.TemaResponse;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = ComumMapper.class)
public interface TemaMapper {

    TemaResponse documentParaResponse(TemaDocument document);

    List<TemaResponse> listaDocumentParaResponse(Iterable<TemaDocument> documents);

}
