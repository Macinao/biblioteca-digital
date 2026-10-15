package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.mapper.MaterialEstudoMapper;
import com.digital.smartteca.search.repository.ConsultaPaginada;
import com.digital.smartteca.search.repository.MaterialEstudoBuscaRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.response.MaterialEstudoResponse;
import com.digital.smartteca.search.treatment.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/material-estudo")
@RequiredArgsConstructor
public class MaterialEstudoController {

    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;
    private final MaterialEstudoBuscaRepository materialEstudoBuscaRepository;
    private final MaterialEstudoMapper materialEstudoMapper;
    private final ConsultaPaginada consulta;

    /** Lista paginada, mais recentes primeiro; o total de itens vem no header X-Total-Count. */
    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<MaterialEstudoResponse>> listar(
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho,
            @RequestParam(required = false) String cursoId) {

        Pageable pageable = Paginacao.de(pagina, tamanho, "dataPublicacao");
        Page<MaterialEstudoDocument> resultado = consulta.listar(MaterialEstudoDocument.class, pageable, "cursos", true, cursoId);

        return Paginacao.resposta(resultado, materialEstudoMapper::listaDocumentParaResponse);

    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<MaterialEstudoResponse> detalhar(@PathVariable String id) {

        return materialEstudoDocumentRepository.findById(id)
                .map(documento -> ResponseEntity.ok(materialEstudoMapper.documentParaResponse(documento)))
                .orElseThrow(() -> new NaoEncontradoException("Material de estudo não encontrado com id: " + id));

    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<MaterialEstudoResponse>> buscar(@RequestParam String texto) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(materialEstudoMapper.listaDocumentParaResponse(materialEstudoBuscaRepository.buscarSemanticamente(texto)));

    }
}
