package com.digital.biblioteca.controller;

import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.response.LivroPublicoResponse;
import com.digital.biblioteca.service.implement.LivroPublicoServiceImpl;
import com.digital.biblioteca.validation.annotation.Imagem;
import com.digital.biblioteca.validation.annotation.ImagemCont;
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
@RequestMapping("/api/v1/livro-publico")
@RequiredArgsConstructor
public class LivroPublicoController {

    private final LivroPublicoServiceImpl livroPublicoService;

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<LivroPublicoResponse> cadastrar(
            @Valid @RequestPart(name = "livro-publico") CadastrarLivroPublicoRequest request,
            @PdfCont @RequestPart(name = "conteudo") MultipartFile conteudo,
            @ImagemCont @RequestPart(name = "capa") MultipartFile capa) throws IOException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(livroPublicoService.cadastrar(request, conteudo, capa));

    }

    @PutMapping("/atualizar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<LivroPublicoResponse> atualizar(
            @Valid @RequestPart("livro-publico") AtualizarLivroPublicoRequest request,
            @Pdf @RequestPart(name = "conteudo", required = false) MultipartFile conteudo,
            @Imagem @RequestPart(name = "capa", required = false) MultipartFile capa) throws IOException {

        return ResponseEntity.status(HttpStatus.OK)
                .body(livroPublicoService.atualizar(request, conteudo, capa));

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {

        livroPublicoService.deletar(id);

        return ResponseEntity.noContent().build();

    }

}
