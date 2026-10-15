package com.digital.biblioteca.controller;

import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.response.TccResponse;
import com.digital.biblioteca.service.implement.TccServiceImpl;
import com.digital.biblioteca.validation.annotation.Pdf;
import com.digital.biblioteca.validation.annotation.PdfCont;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tcc")
@RequiredArgsConstructor
public class TccController {

    private final TccServiceImpl tccService;

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<TccResponse> cadastrar(
            @Valid @RequestPart(name = "tcc") CadastrarTccRequest request,
            @PdfCont @RequestPart(name = "conteudo") MultipartFile conteudo) throws IOException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tccService.cadastrar(request, conteudo));

    }

    @PutMapping("/atualizar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<TccResponse> atualizar(
            @Valid @RequestPart("tcc") AtualizarTccRequest request,
            @Pdf @RequestPart(name = "conteudo", required = false) MultipartFile conteudo) throws IOException {

        return ResponseEntity.status(HttpStatus.OK)
                .body(tccService.atualizar(request, conteudo));

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {

        tccService.deletar(id);

        return ResponseEntity.noContent().build();

    }

}
