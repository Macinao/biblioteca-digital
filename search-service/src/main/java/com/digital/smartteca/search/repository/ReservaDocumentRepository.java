package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.ReservaDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface ReservaDocumentRepository extends ElasticsearchRepository<ReservaDocument, String> {

    List<ReservaDocument> findByMatriculaUsuario(String matricula);

}
