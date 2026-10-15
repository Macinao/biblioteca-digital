package com.digital.smartteca.notification.service;

import com.digital.smartteca.notification.config.EvolutionProperties;
import com.digital.smartteca.notification.request.ReceberMensagemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * O WebClient agora e injetado (um bean unico, ver WebClientConfig) em
 * vez de criado a cada chamada, como era no MetodosMensagem original do
 * core-service.
 */
@Service
@RequiredArgsConstructor
public class MetodosMensagemImpl implements MetodosMensagem {

    private final WebClient evolutionWebClient;
    private final EvolutionProperties evolutionProperties;

    @Override
    public Mono<String> enviarMensagem(String numero, String texto) {

        ReceberMensagemRequest request = new ReceberMensagemRequest(numero, texto);

        return evolutionWebClient.post()
                .uri("/message/sendText/" + evolutionProperties.getInstance())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class);

    }

}
