package com.digital.smartteca.search.mapper;

import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.response.LivroResponse;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class})
public interface LivroMapper {

    LivroResponse documentParaResponse(LivroDocument document);

    List<LivroResponse> listaDocumentParaResponse(Iterable<LivroDocument> documents);

}
