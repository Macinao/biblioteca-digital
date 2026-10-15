package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.mapper.TemaMapper;
import com.digital.smartteca.search.repository.TemaDocumentRepository;
import com.digital.smartteca.search.response.TemaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tema")
@RequiredArgsConstructor
public class TemaController {

    private final TemaDocumentRepository temaDocumentRepository;
    private final TemaMapper temaMapper;

    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<TemaResponse>> listar() {

        return ResponseEntity.status(HttpStatus.OK)
                .body(temaMapper.listaDocumentParaResponse(temaDocumentRepository.findAll()));

    }
}
