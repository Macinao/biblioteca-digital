package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.LivroDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface LivroDocumentRepository extends ElasticsearchRepository<LivroDocument, String> {

    List<LivroDocument> findByTemasId(String id);

    List<LivroDocument> findByCursosId(String id);

}
