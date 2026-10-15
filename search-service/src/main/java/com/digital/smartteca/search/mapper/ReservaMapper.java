package com.digital.smartteca.search.mapper;

import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.response.ReservaResponse;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = ComumMapper.class)
public interface ReservaMapper {

    ReservaResponse documentParaResponse(ReservaDocument document);

    List<ReservaResponse> listaDocumentParaResponse(Iterable<ReservaDocument> documents);

}
