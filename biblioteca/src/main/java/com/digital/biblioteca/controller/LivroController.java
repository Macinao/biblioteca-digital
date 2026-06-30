package com.digital.biblioteca.controller;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.request.AtualizarLivroRequest;
import com.digital.biblioteca.request.CadastrarLivroRequest;
import com.digital.biblioteca.response.BaixarResponse;
import com.digital.biblioteca.response.LivroResponse;
import com.digital.biblioteca.service.implement.LivroServiceImpl;
import com.digital.biblioteca.service.implement.SupabaseServiceImpl;
import com.digital.biblioteca.validation.annotation.Imagem;
import com.digital.biblioteca.validation.annotation.ImagemCont;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/livro")
@RequiredArgsConstructor
public class LivroController {

    private final LivroServiceImpl livroService;
    private final SupabaseServiceImpl supabaseService;

    @PostMapping("/cadastrar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<LivroResponse> cadastrar(
            @Valid @RequestPart(name = "livro") CadastrarLivroRequest request,
            @ImagemCont @RequestPart(name = "capa") MultipartFile capa) throws IOException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(livroService.cadastrar(request, capa));

    }

    @PutMapping("/atualizar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<LivroResponse> atualizar(
            @Valid @RequestPart("livro") AtualizarLivroRequest request,
            @Imagem @RequestPart(name = "capa", required = false) MultipartFile capa) throws IOException {

        return ResponseEntity.status(HttpStatus.OK)
                .body(livroService.atualizar(request, capa));

    }

    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<LivroResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(livroService.listar());

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {

        livroService.deletar(id);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/baixar/capa/{url}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<Resource> baixarCapa(
            @PathVariable String url) {

        BaixarResponse capa = supabaseService.baixar(url,
                BucketSupabase.LIVRO_CAPA);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(capa.getContentType()))
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + capa.getNome() + "\"")
                .body(capa.getResource());

    }
}
