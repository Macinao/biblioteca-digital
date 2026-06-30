package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.MaterialEstudoDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface MaterialEstudoDocumentRepository
        extends ElasticsearchRepository<MaterialEstudoDocument, String>, MaterialEstudoBuscaRepository {

    List<MaterialEstudoDocument> findByTemasId(String id);

    List<MaterialEstudoDocument> findByCursosId(String id);

}
