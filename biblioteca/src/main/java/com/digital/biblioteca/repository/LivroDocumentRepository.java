package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.LivroDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface LivroDocumentRepository
        extends ElasticsearchRepository<LivroDocument, String>, LivroBuscaRepository {

    List<LivroDocument> findByTemasId(String id);

    List<LivroDocument> findByCursosId(String id);

}
