package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.mapper.TccMapper;
import com.digital.smartteca.search.repository.ConsultaPaginada;
import com.digital.smartteca.search.repository.TccBuscaRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import com.digital.smartteca.search.response.TccResponse;
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
@RequestMapping("/api/v1/tcc")
@RequiredArgsConstructor
public class TccController {

    private final TccDocumentRepository tccDocumentRepository;
    private final TccBuscaRepository tccBuscaRepository;
    private final TccMapper tccMapper;
    private final ConsultaPaginada consulta;

    /** Lista paginada, mais recentes primeiro; o total de itens vem no header X-Total-Count. */
    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<TccResponse>> listar(
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho,
            @RequestParam(required = false) String cursoId) {

        Pageable pageable = Paginacao.de(pagina, tamanho, "dataPublicacao");
        Page<TccDocument> resultado = consulta.listar(TccDocument.class, pageable, "curso", false, cursoId);

        return Paginacao.resposta(resultado, tccMapper::listaDocumentParaResponse);

    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<TccResponse> detalhar(@PathVariable String id) {

        return tccDocumentRepository.findById(id)
                .map(documento -> ResponseEntity.ok(tccMapper.documentParaResponse(documento)))
                .orElseThrow(() -> new NaoEncontradoException("TCC não encontrado com id: " + id));

    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<TccResponse>> buscar(@RequestParam String texto) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(tccMapper.listaDocumentParaResponse(tccBuscaRepository.buscarSemanticamente(texto)));

    }
}
