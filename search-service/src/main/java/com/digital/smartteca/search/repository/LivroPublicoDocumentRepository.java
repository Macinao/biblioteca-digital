package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.LivroPublicoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface LivroPublicoDocumentRepository extends ElasticsearchRepository<LivroPublicoDocument, String> {

    List<LivroPublicoDocument> findByTemasId(String id);

    List<LivroPublicoDocument> findByCursosId(String id);

}
