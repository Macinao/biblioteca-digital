package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.mapper.CursoMapper;
import com.digital.smartteca.search.repository.CursoDocumentRepository;
import com.digital.smartteca.search.response.CursoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/curso")
@RequiredArgsConstructor
public class CursoController {

    private final CursoDocumentRepository cursoDocumentRepository;
    private final CursoMapper cursoMapper;

    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<CursoResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(cursoMapper.listaDocumentParaResponse(cursoDocumentRepository.findAll()));

    }
}
