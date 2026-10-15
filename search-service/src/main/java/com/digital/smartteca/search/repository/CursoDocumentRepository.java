package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.CursoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CursoDocumentRepository extends ElasticsearchRepository<CursoDocument, String> {
}
