package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.ReservaDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ReservaDocumentRepository extends ElasticsearchRepository<ReservaDocument, String> {

    long countByMatriculaUsuario(String matricula);

    List<ReservaDocument> findByMatriculaUsuario(String matricula);

}
