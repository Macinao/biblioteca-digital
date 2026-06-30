package com.digital.biblioteca.controller;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.response.BaixarResponse;
import com.digital.biblioteca.response.LivroPublicoResponse;
import com.digital.biblioteca.service.implement.LivroPublicoServiceImpl;
import com.digital.biblioteca.service.implement.SupabaseServiceImpl;
import com.digital.biblioteca.validation.annotation.Imagem;
import com.digital.biblioteca.validation.annotation.ImagemCont;
import com.digital.biblioteca.validation.annotation.Pdf;
import com.digital.biblioteca.validation.annotation.PdfCont;
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
@RequestMapping("/api/v1/livro-publico")
@RequiredArgsConstructor
public class LivroPublicoController {

    private final LivroPublicoServiceImpl livroPublicoService;
    private final SupabaseServiceImpl supabaseService;

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

    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<LivroPublicoResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(livroPublicoService.listar());

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {

        livroPublicoService.deletar(id);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/baixar/capa/{url}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<Resource> baixarCapa(
            @PathVariable String url) {

        BaixarResponse capa = supabaseService.baixar(url,
                BucketSupabase.LIVRO_PUBLICO_CAPA);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(capa.getContentType()))
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + capa.getNome() + "\"")
                .body(capa.getResource());

    }

    @GetMapping("/baixar/conteudo/{url}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<Resource> baixarConteudo(
            @PathVariable String url) {

        BaixarResponse conteudo = supabaseService.baixar(url,
                BucketSupabase.LIVRO_PUBLICO_CONTEUDO);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(conteudo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + conteudo.getNome() + "\"")
                .body(conteudo.getResource());

    }
}
