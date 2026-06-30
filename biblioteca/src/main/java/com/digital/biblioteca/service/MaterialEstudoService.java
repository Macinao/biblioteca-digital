package com.digital.biblioteca.service;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.response.MaterialEstudoResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface MaterialEstudoService {

    MaterialEstudoResponse cadastrar(CadastrarMaterialEstudoRequest request,
                                     MultipartFile conteudo,
                                     Usuario usuario);

    MaterialEstudoResponse atualizar(AtualizarMaterialEstudoRequest request,
                                     MultipartFile conteudo,
                                     Usuario usuario);

    List<MaterialEstudoResponse> listar();

    void deletar(UUID id, Usuario usuario);

}
