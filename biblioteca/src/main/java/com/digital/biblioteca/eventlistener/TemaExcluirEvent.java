package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.TemaDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TemaExcluirEvent {

    private final TemaDocument tema;

}
