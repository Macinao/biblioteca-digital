package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.CursoDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CursoAtualizarEvent {

    private final CursoDocument curso;

}
