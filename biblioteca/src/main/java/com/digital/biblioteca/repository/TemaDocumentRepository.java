package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.TemaDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TemaDocumentRepository extends ElasticsearchRepository<TemaDocument, String> {
}
