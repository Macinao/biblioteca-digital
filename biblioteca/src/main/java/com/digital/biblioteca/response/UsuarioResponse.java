package com.digital.biblioteca.response;

import com.digital.biblioteca.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
public class UsuarioResponse {

    private UUID id;

    private String nome;

    private String matricula;

    private String email;

    private String telefone;

    private Role role;

}
