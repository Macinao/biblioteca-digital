package com.digital.biblioteca.service;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.DevolverRequest;
import com.digital.biblioteca.request.ReservarRequest;
import com.digital.biblioteca.response.ReservaResponse;
import java.util.List;

public interface ReservaService {

    ReservaResponse reservar (ReservarRequest request);

    void devolver (DevolverRequest request);

    List<ReservaResponse> listar();

    List<ReservaResponse> listarPorUsuario(Usuario usuario);

}
