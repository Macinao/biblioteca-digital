package com.digital.smartteca.search.mapper;

import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.response.LivroPublicoResponse;
import org.mapstruct.Mapper;
import java.util.List;

@Mapper(componentModel = "spring", uses = {CursoMapper.class, TemaMapper.class, ComumMapper.class})
public interface LivroPublicoMapper {

    LivroPublicoResponse documentParaResponse(LivroPublicoDocument document);

    List<LivroPublicoResponse> listaDocumentParaResponse(Iterable<LivroPublicoDocument> documents);

}
