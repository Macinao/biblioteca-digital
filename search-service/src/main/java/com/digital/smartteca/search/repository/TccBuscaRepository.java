package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.TccDocument;
import java.util.List;

public interface TccBuscaRepository {

    List<TccDocument> buscarSemanticamente(String texto);

}
