package com.digital.biblioteca.repository;

import com.digital.biblioteca.document.TccDocument;

import java.util.List;

public interface TccBuscaRepository {

    List<TccDocument> buscarSemanticamente(String texto);

}
