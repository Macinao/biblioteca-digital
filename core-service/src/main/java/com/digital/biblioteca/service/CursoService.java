package com.digital.biblioteca.service;

import com.digital.biblioteca.request.AtualizarCursoRequest;
import com.digital.biblioteca.request.CadastrarCursoRequest;
import com.digital.biblioteca.response.CursoResponse;
import java.util.UUID;

public interface CursoService {

    CursoResponse cadastrar(CadastrarCursoRequest request);

    CursoResponse atualizar(AtualizarCursoRequest request);

    void deletar(UUID id);

}
