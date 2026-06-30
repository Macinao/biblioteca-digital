package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.CursoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CursoDocumentRepository extends ElasticsearchRepository<CursoDocument, String> {
}
