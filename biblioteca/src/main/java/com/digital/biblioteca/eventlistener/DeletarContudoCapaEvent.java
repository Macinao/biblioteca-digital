package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.enums.BucketSupabase;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeletarContudoCapaEvent {

    private final String conteudo;

    private final String capa;

    private final BucketSupabase bucketConteudo;

    private final BucketSupabase bucketCapa;

}
