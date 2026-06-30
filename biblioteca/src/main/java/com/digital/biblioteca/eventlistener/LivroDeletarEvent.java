package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.LivroDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LivroDeletarEvent {

    private final LivroDocument livroDocument;

}
