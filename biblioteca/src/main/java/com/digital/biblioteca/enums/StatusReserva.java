package com.digital.biblioteca.enums;

public enum StatusReserva {
    RESERVADO("RESERVADO"),
    ATRASADO("ATRASADO");

    private final String name;

    StatusReserva(String name) {
        this.name = name;
    }
}
