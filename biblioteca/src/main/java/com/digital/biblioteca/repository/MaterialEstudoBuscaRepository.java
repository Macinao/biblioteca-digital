package com.digital.biblioteca.repository;

import com.tecnico.projeto.tcc.document.MaterialEstudoDocument;

import java.util.List;

public interface MaterialEstudoBuscaRepository {

    List<MaterialEstudoDocument> buscarSemanticamente(String texto);

}
