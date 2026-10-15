package com.digital.smartteca.storage.service;

import com.digital.smartteca.common.enums.BucketSupabase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupabaseService {

    Mono<Void> subir(byte[] conteudo, String contentType, BucketSupabase bucket, String nome);

    Mono<ResponseEntity<Flux<DataBuffer>>> baixar(String nome, BucketSupabase bucket);

    Mono<Void> deletar(String nome, BucketSupabase bucket);

}
