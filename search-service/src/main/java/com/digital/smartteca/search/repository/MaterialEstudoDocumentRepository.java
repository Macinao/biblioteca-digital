package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.MaterialEstudoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface MaterialEstudoDocumentRepository extends ElasticsearchRepository<MaterialEstudoDocument, String> {

    List<MaterialEstudoDocument> findByTemasId(String id);

    List<MaterialEstudoDocument> findByCursosId(String id);

}
