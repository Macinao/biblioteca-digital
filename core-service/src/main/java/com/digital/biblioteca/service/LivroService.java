package com.digital.biblioteca.service;

import com.digital.biblioteca.request.*;
import com.digital.biblioteca.response.LivroResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public interface LivroService {

    LivroResponse cadastrar(CadastrarLivroRequest request,
                            MultipartFile capa) throws IOException;

    LivroResponse atualizar(AtualizarLivroRequest request,
                            MultipartFile capa) throws IOException;

    void deletar(UUID id);

}
