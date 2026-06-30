package com.digital.biblioteca.enums;

import lombok.Getter;

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
