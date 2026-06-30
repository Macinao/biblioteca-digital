package com.digital.biblioteca.eventlistener;

import com.tecnico.projeto.tcc.document.CursoDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CursoExcluirEvent {

    private final CursoDocument curso;

}
