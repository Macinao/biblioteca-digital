package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.ReservaDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DevolverEvent {

    private final ReservaDocument reservaDocument;

}
