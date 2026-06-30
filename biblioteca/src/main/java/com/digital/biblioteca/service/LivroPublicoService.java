package com.digital.biblioteca.service;

import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.response.LivroPublicoResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface LivroPublicoService {

    LivroPublicoResponse cadastrar(CadastrarLivroPublicoRequest request,
                                   MultipartFile conteudo, MultipartFile capa);

    LivroPublicoResponse atualizar(AtualizarLivroPublicoRequest request,
                                   MultipartFile conteudo, MultipartFile capa);

    List<LivroPublicoResponse> listar();

    void deletar(UUID id);

}
