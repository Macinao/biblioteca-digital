package com.digital.biblioteca.service;

import com.digital.biblioteca.request.CadastrarUsuarioRequest;
import com.digital.biblioteca.request.LoginUsuarioRequest;
import com.digital.biblioteca.response.TokenAcessoResponse;

public interface AuthService {

    TokenAcessoResponse login(LoginUsuarioRequest request);

    TokenAcessoResponse refresh(String refreshToken);

    void cadastro(CadastrarUsuarioRequest request);

}
