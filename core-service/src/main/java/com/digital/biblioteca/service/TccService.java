package com.digital.biblioteca.service;

import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.response.TccResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public interface TccService {

    TccResponse cadastrar(CadastrarTccRequest request,
                          MultipartFile conteudo) throws IOException;

    TccResponse atualizar(AtualizarTccRequest request,
                          MultipartFile conteudo) throws IOException;

    void deletar(UUID id);

}
