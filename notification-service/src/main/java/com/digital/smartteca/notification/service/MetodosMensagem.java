package com.digital.smartteca.notification.service;

import reactor.core.publisher.Mono;

public interface MetodosMensagem {

    Mono<String> enviarMensagem(String numero, String texto);

}
