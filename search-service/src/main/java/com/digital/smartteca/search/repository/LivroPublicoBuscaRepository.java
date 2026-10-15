package com.digital.smartteca.search.repository;

import com.digital.smartteca.search.document.LivroPublicoDocument;
import java.util.List;

public interface LivroPublicoBuscaRepository {

    List<LivroPublicoDocument> buscarSemanticamente(String texto);

}
