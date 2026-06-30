package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.enums.BucketSupabase;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeletarArquivoEvent {

    private final String arquivo;

    private final BucketSupabase bucketArquivo;

}
