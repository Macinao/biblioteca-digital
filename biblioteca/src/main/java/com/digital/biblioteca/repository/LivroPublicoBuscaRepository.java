package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.LivroPublicoDocument;
import java.util.List;

public interface LivroPublicoBuscaRepository {

    List<LivroPublicoDocument> buscarSemanticamente(String texto);

}
