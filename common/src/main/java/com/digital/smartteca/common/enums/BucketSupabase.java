package com.digital.smartteca.common.enums;

import lombok.Getter;

/**
 * Compartilhado entre core-service (que decide em qual bucket cada arquivo
 * vai, ao montar o comando) e storage-service (que efetivamente fala com
 * o Supabase usando esse valor) — mesmo raciocinio do StatusReserva.
 */
@Getter
public enum BucketSupabase {

    LIVRO_CAPA("livro-capa"),
    LIVRO_PUBLICO_CONTEUDO("livro-publico-conteudo"),
    LIVRO_PUBLICO_CAPA("livro-publico-capa"),
    MATERIAL_CONTEUDO("material-conteudo"),
    TCC_CONTEUDO("tcc-conteudo");

    private final String bucket;

    BucketSupabase(String bucket) {
        this.bucket = bucket;
    }

}
