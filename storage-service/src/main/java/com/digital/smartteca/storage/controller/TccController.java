package com.digital.smartteca.storage.controller;

import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.smartteca.storage.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/tcc")
@RequiredArgsConstructor
public class TccController {

    private final SupabaseService supabaseService;

    @GetMapping("/baixar/conteudo/{nome}")
    @PreAuthorize("hasRole('ALUNO')")
    public Mono<ResponseEntity<Flux<DataBuffer>>> baixarConteudo(@PathVariable String nome) {

        return supabaseService.baixar(nome, BucketSupabase.TCC_CONTEUDO);

    }
}
