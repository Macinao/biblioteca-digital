package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.model.Tema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TemaCadastroEvent {

    private final Tema tema;

}
