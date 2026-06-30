package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.TccDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TccCadastrarEvent {

    private final TccDocument tccDocument;

}
