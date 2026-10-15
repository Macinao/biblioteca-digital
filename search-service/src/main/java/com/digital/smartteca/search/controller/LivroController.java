package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.mapper.LivroMapper;
import com.digital.smartteca.search.repository.ConsultaPaginada;
import com.digital.smartteca.search.repository.LivroBuscaRepository;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.response.LivroResponse;
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
@RequestMapping("/api/v1/livro")
@RequiredArgsConstructor
public class LivroController {

    private final LivroDocumentRepository livroDocumentRepository;
    private final LivroBuscaRepository livroBuscaRepository;
    private final LivroMapper livroMapper;
    private final ConsultaPaginada consulta;

    /** Lista paginada, mais recentes primeiro; o total de itens vem no header X-Total-Count. */
    @GetMapping("/listar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<LivroResponse>> listar(
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho,
            @RequestParam(required = false) String cursoId) {

        Pageable pageable = Paginacao.de(pagina, tamanho, "dataPublicacao");
        Page<LivroDocument> resultado = consulta.listar(LivroDocument.class, pageable, "cursos", true, cursoId);

        return Paginacao.resposta(resultado, livroMapper::listaDocumentParaResponse);

    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<LivroResponse> detalhar(@PathVariable String id) {

        return livroDocumentRepository.findById(id)
                .map(documento -> ResponseEntity.ok(livroMapper.documentParaResponse(documento)))
                .orElseThrow(() -> new NaoEncontradoException("Livro não encontrado com id: " + id));

    }

    @GetMapping("/buscar")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<LivroResponse>> buscar(@RequestParam String texto) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(livroMapper.listaDocumentParaResponse(livroBuscaRepository.buscarSemanticamente(texto)));

    }
}
