package com.digital.biblioteca.service;

import com.digital.biblioteca.request.AtualizarTemaRequest;
import com.digital.biblioteca.request.CadastrarTemaRequest;
import com.digital.biblioteca.response.TemaResponse;
import java.util.List;
import java.util.UUID;

public interface TemaService {

    TemaResponse cadastrar(CadastrarTemaRequest request);

    TemaResponse atualizar(AtualizarTemaRequest request);

    List<TemaResponse> listar();

    void deletar(UUID id);

}
