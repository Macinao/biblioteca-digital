package com.digital.biblioteca.service;

import com.digital.biblioteca.request.*;
import com.digital.biblioteca.response.LivroResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface LivroService {

    LivroResponse cadastrar(CadastrarLivroRequest request,
                            MultipartFile capa);

    LivroResponse atualizar(AtualizarLivroRequest request,
                            MultipartFile capa);

    List<LivroResponse> listar();

    void deletar(UUID id);

}
