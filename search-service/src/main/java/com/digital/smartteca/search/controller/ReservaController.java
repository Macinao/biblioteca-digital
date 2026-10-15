package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.mapper.ReservaMapper;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.repository.ConsultaPaginada;
import com.digital.smartteca.search.repository.ReservaDocumentRepository;
import com.digital.smartteca.search.response.ReservaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reserva")
@RequiredArgsConstructor
public class ReservaController {

    private final ReservaDocumentRepository reservaDocumentRepository;
    private final ReservaMapper reservaMapper;
    private final ConsultaPaginada consulta;

    @GetMapping("/listar")
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<List<ReservaResponse>> listar(
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamanho) {

        return Paginacao.resposta(
                consulta.listar(ReservaDocument.class, Paginacao.de(pagina, tamanho, "dataCriacao")),
                reservaMapper::listaDocumentParaResponse);

    }

    @GetMapping("/listar-minhas")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<ReservaResponse>> listarMinhas(Authentication authentication) {

        // O GatewayHeaderAuthFilter coloca a matricula direto como principal
        // (nao ha Usuario aqui, search-service nao tem essa tabela).
        String matricula = authentication.getName();

        return ResponseEntity.status(HttpStatus.OK)
                .body(reservaMapper.listaDocumentParaResponse(
                        reservaDocumentRepository.findByMatriculaUsuario(matricula)));

    }
}
