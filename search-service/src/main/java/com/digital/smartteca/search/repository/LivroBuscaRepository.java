package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.LivroDocument;
import java.util.List;

public interface LivroBuscaRepository {

    List<LivroDocument> buscarSemanticamente(String texto);

}
