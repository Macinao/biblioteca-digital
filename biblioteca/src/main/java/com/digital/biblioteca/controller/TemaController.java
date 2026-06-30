package com.digital.biblioteca.controller;

import com.digital.biblioteca.request.AtualizarTemaRequest;
import com.digital.biblioteca.request.CadastrarTemaRequest;
import com.digital.biblioteca.response.TemaResponse;
import com.digital.biblioteca.service.implement.TemaServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tema")
@RequiredArgsConstructor
public class TemaController {

    private final TemaServiceImpl temaService;

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<TemaResponse> cadastrar(
            @RequestBody @Valid CadastrarTemaRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(temaService.cadastrar(request));

    }

    @PutMapping("/atualizar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<TemaResponse> atualizar(
            @RequestBody @Valid AtualizarTemaRequest request) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(temaService.atualizar(request));

    }

    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<TemaResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(temaService.listar());

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deletar(
            @PathVariable UUID id) {

        temaService.deletar(id);

        return ResponseEntity.noContent().build();

    }
}
