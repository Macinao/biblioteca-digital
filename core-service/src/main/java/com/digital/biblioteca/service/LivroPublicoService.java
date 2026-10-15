package com.digital.biblioteca.service;

import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.response.LivroPublicoResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public interface LivroPublicoService {

    LivroPublicoResponse cadastrar(CadastrarLivroPublicoRequest request,
                                   MultipartFile conteudo, MultipartFile capa) throws IOException;

    LivroPublicoResponse atualizar(AtualizarLivroPublicoRequest request,
                                   MultipartFile conteudo, MultipartFile capa) throws IOException;

    void deletar(UUID id);

}
