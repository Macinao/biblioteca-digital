package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.LivroDocument;
import java.util.List;

public interface LivroBuscaRepository {

    List<LivroDocument> buscarSemanticamente(String texto);

}
