package com.digital.biblioteca.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class TokenAcessoResponse {

    String tokenAcesso;

    String tokenRefresh;

}
