package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.LivroPublicoDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LivroPublicoAtualizarEvent {

    private final LivroPublicoDocument livroPublicoDocument;

}
