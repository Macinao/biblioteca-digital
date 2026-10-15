package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.TccDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface TccDocumentRepository extends ElasticsearchRepository<TccDocument, String> {

    List<TccDocument> findByTemasId(String id);

    // Singular (curso, nao cursos) porque o campo no documento tambem e singular.
    List<TccDocument> findByCursoId(String id);

}
