package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.TemaDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TemaDocumentRepository extends ElasticsearchRepository<TemaDocument, String> {
}
