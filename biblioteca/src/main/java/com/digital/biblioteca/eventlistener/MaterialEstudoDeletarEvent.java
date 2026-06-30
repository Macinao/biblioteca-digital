package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.MaterialEstudoDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MaterialEstudoDeletarEvent {

    public final MaterialEstudoDocument materialEstudoDocument;

}
