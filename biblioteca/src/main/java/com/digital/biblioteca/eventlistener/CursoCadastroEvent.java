package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.model.Curso;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CursoCadastroEvent {

    private final Curso curso;

}
