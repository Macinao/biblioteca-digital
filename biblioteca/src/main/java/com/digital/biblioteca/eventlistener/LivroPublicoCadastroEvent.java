package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.model.LivroPublico;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LivroPublicoCadastroEvent {

    private final LivroPublico livroPublico;

}
