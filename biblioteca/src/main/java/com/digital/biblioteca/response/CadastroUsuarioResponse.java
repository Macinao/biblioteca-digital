package com.digital.biblioteca.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class CadastroUsuarioResponse {

    String tokenAcesso;
    String tokenRefresh;

}
