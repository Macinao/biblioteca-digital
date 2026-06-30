package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.LivroPublicoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface LivroPublicoDocumentRepository
        extends ElasticsearchRepository<LivroPublicoDocument, String>, LivroPublicoBuscaRepository{

    List<LivroPublicoDocument> findByTemasId(String id);

    List<LivroPublicoDocument> findByCursosId(String id);

}
