package com.digital.biblioteca.treatment;

public class IntegracaoExternaException extends RuntimeException {

    public IntegracaoExternaException(String mensagem) {
        super(mensagem);
    }

    public IntegracaoExternaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

}
