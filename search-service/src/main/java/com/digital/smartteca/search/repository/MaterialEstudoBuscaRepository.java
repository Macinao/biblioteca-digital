package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.MaterialEstudoDocument;
import java.util.List;

public interface MaterialEstudoBuscaRepository {

    List<MaterialEstudoDocument> buscarSemanticamente(String texto);

}
