package com.digital.biblioteca.controller;

import com.digital.biblioteca.request.*;
import com.digital.biblioteca.response.CursoResponse;
import com.digital.biblioteca.service.implement.CursoServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curso")
@RequiredArgsConstructor
public class CursoController {

    private final CursoServiceImpl cursoService;

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<CursoResponse> cadastrar(
            @RequestBody @Valid CadastrarCursoRequest request){

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cursoService.cadastrar(request));
    }

    @PutMapping("/atualizar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<CursoResponse> atualizar(
            @RequestBody @Valid AtualizarCursoRequest request) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(cursoService.atualizar(request));
    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deletar(
            @PathVariable UUID id) {

        cursoService.deletar(id);

        return ResponseEntity.noContent().build();
    }

}
