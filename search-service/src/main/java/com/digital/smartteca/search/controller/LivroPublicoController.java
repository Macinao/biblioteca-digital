package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.mapper.LivroPublicoMapper;
import com.digital.smartteca.search.repository.ConsultaPaginada;
import com.digital.smartteca.search.repository.LivroPublicoBuscaRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.response.LivroPublicoResponse;
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
@RequestMapping("/api/v1/livro-publico")
@RequiredArgsConstructor
public class LivroPublicoController {

    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final LivroPublicoBuscaRepository livroPublicoBuscaRepository;
    private final LivroPublicoMapper livroPublicoMapper;
    private final ConsultaPaginada consulta;

    /** Lista paginada, mais recentes primeiro; o total de itens vem no header X-Total-Count. */
    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<LivroPublicoResponse>> listar(
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho,
            @RequestParam(required = false) String cursoId) {

        Pageable pageable = Paginacao.de(pagina, tamanho, "dataPublicacao");
        Page<LivroPublicoDocument> resultado = consulta.listar(LivroPublicoDocument.class, pageable, "cursos", true, cursoId);

        return Paginacao.resposta(resultado, livroPublicoMapper::listaDocumentParaResponse);

    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<LivroPublicoResponse> detalhar(@PathVariable String id) {

        return livroPublicoDocumentRepository.findById(id)
                .map(documento -> ResponseEntity.ok(livroPublicoMapper.documentParaResponse(documento)))
                .orElseThrow(() -> new NaoEncontradoException("Livro público não encontrado com id: " + id));

    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<LivroPublicoResponse>> buscar(@RequestParam String texto) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(livroPublicoMapper.listaDocumentParaResponse(livroPublicoBuscaRepository.buscarSemanticamente(texto)));

    }
}
