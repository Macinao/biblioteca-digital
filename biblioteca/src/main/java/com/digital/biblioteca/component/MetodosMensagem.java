package com.digital.biblioteca.component;

import com.digital.biblioteca.config.evolution.EvolutionProperties;
import com.digital.biblioteca.request.ReceberMensagemRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class MetodosMensagem {

    private final EvolutionProperties properties;

    public String enviarMensagem(String numero, String texto) {

        WebClient client = WebClient.builder()
                .baseUrl(properties.getUrl())
                .defaultHeader("apikey", properties.getApikey())
                .build();

        ReceberMensagemRequest request =
                new ReceberMensagemRequest(numero, texto);

        return client.post()
                .uri("/message/sendText/" + properties.getInstance())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
