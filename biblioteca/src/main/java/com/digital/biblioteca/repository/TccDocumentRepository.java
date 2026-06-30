package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.TccDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface TccDocumentRepository
        extends ElasticsearchRepository<TccDocument, String>, TccBuscaRepository {

    List<TccDocument> findByTemasId(String id);

    List<TccDocument> findByCursoId(String id);

}
