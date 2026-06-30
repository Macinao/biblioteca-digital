package com.digital.biblioteca.controller;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.response.BaixarResponse;
import com.digital.biblioteca.response.TccResponse;
import com.digital.biblioteca.service.implement.SupabaseServiceImpl;
import com.digital.biblioteca.service.implement.TccServiceImpl;
import com.digital.biblioteca.validation.annotation.Pdf;
import com.digital.biblioteca.validation.annotation.PdfCont;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tcc")
@RequiredArgsConstructor
public class TccController {

    private final TccServiceImpl tccService;
    private final SupabaseServiceImpl supabaseService;

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

    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<TccResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(tccService.listar());

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {

        tccService.deletar(id);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/baixar/conteudo/{url}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<Resource> baixarConteudo(
            @PathVariable String url) {

        BaixarResponse conteudo = supabaseService.baixar(url,
                BucketSupabase.TCC_CONTEUDO);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(conteudo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + conteudo.getNome() + "\"")
                .body(conteudo.getResource());

    }
}
