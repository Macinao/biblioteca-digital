package com.digital.biblioteca.controller;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.DevolverRequest;
import com.digital.biblioteca.request.ReservarRequest;
import com.digital.biblioteca.response.ReservaResponse;
import com.digital.biblioteca.service.implement.ReservaServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reserva")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaServiceImpl reservaService;

    @PostMapping("/reservar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<ReservaResponse> reservar(
            @Valid @RequestBody ReservarRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservaService.reservar(request));

    }

    @PutMapping("/devolver")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> devolver(
            @Valid @RequestBody DevolverRequest request) {

            reservaService.devolver(request);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/listar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<List<ReservaResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(reservaService.listar());

    }

    @GetMapping("/listar-minhas")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<ReservaResponse>> minhasReservas(@AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(reservaService.listarPorUsuario(usuario));

    }

}
