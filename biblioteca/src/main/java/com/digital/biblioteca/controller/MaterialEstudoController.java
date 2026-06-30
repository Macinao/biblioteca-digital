package com.digital.biblioteca.controller;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.response.BaixarResponse;
import com.digital.biblioteca.response.MaterialEstudoResponse;
import com.digital.biblioteca.service.implement.MaterialEstudoServiceImpl;
import com.digital.biblioteca.service.implement.SupabaseServiceImpl;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/material-estudo")
@RequiredArgsConstructor
public class MaterialEstudoController {

    private final MaterialEstudoServiceImpl materialEstudoService;
    private final SupabaseServiceImpl supabaseService;

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

    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<MaterialEstudoResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(materialEstudoService.listar());

    }

    @DeleteMapping("/deletar/{id}")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<Void> deletar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Usuario usuario) {

        materialEstudoService.deletar(id, usuario);

        return ResponseEntity.noContent().build();

    }

    @GetMapping("/baixar/conteudo/{url}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<Resource> baixarConteudo(
            @PathVariable String url) {

        BaixarResponse conteudo = supabaseService.baixar(url,
                BucketSupabase.MATERIAL_CONTEUDO);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(conteudo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + conteudo.getNome() + "\"")
                .body(conteudo.getResource());

    }
}
