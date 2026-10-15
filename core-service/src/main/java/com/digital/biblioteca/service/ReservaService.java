package com.digital.biblioteca.service;

import com.digital.biblioteca.request.DevolverRequest;
import com.digital.biblioteca.request.ReservarRequest;
import com.digital.biblioteca.response.ReservaResponse;

public interface ReservaService {

    ReservaResponse reservar (ReservarRequest request);

    void devolver (DevolverRequest request);

}
