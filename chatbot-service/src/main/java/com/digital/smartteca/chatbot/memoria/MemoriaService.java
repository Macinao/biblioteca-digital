package com.digital.smartteca.chatbot.memoria;

import java.util.List;

public interface MemoriaService {

    void adicionarMensagem(String matricula, String role, String conteudo);

    List<Mensagem> buscarUltimasMensagens(String matricula, int limite);

}
