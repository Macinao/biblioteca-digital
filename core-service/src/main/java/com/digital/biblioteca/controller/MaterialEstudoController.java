package com.digital.biblioteca.controller;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.response.MaterialEstudoResponse;
import com.digital.biblioteca.service.implement.MaterialEstudoServiceImpl;
import com.digital.biblioteca.validation.annotation.Pdf;
import com.digital.biblioteca.validation.annotation.PdfCont;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/material-estudo")
@RequiredArgsConstructor
public class MaterialEstudoController {

    private final MaterialEstudoServiceImpl materialEstudoService;

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<MaterialEstudoResponse> cadastrar(
            @Valid @RequestPart(name = "material-estudo") CadastrarMaterialEstudoRequest request,
            @PdfCont @RequestPart(name = "conteudo") MultipartFile conteudo,
            @AuthenticationPrincipal Usuario usuario) throws IOException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialEstudoService.cadastrar(request, conteudo, usuario));

    }

    @PutMapping("/atualizar")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<MaterialEstudoResponse> atualizar(
            @Valid @RequestPart("material-estudo") AtualizarMaterialEstudoRequest request,
            @Pdf @RequestPart(name = "conteudo", required = false) MultipartFile conteudo,
            @AuthenticationPrincipal Usuario usuario) throws IOException {

        return ResponseEntity.status(HttpStatus.OK)
                .body(materialEstudoService.atualizar(request, conteudo, usuario));

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Void> deletar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuario) {

        materialEstudoService.deletar(id, usuario);

        return ResponseEntity.noContent().build();

    }

}
