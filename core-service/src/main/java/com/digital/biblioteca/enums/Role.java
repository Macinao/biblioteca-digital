package com.digital.biblioteca.enums;

import lombok.Getter;

@Getter
public enum Role {
    ALUNO("ROLE_ALUNO"),
    PROFESSOR("ROLE_PROFESSOR"),
    BIBLIOTECARIO("ROLE_BIBLIOTECARIO");

    private final String role;

    Role(String role) {
        this.role = role;
    }
}
