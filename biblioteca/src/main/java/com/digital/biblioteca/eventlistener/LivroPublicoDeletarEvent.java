package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.LivroPublicoDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LivroPublicoDeletarEvent {

    private final LivroPublicoDocument livroPublicoDocument;

}
