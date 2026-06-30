package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.MemoriaDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MemoriaDocumentRepository extends ElasticsearchRepository<MemoriaDocument, String> {
}
