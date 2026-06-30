package com.digital.biblioteca.service;

import com.digital.biblioteca.document.MemoriaDocument;

public interface MemoriaService {

    void adicionarMensagem(String id, String role, String conteudo);

    MemoriaDocument buscar(String id);

}
