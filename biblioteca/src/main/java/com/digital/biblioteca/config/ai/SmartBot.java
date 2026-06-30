package com.digital.biblioteca.config.ai;

public interface SmartBot {

    String chat(
            String nomeUsuario,
            String historico,
            String mensagem
    );

}
