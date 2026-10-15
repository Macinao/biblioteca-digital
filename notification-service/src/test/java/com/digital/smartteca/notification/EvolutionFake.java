package com.digital.smartteca.notification;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Servidor HTTP que imita a Evolution API: grava o que recebe e responde com o status configurado. */
public final class EvolutionFake {

    public record Recebida(String metodo, String caminho, String apikey, String contentType, String corpo) {
    }

    public final ConcurrentLinkedQueue<Recebida> recebidas = new ConcurrentLinkedQueue<>();
    public volatile int status = 201;
    public volatile String resposta = "{\"key\":{\"id\":\"ABC\"},\"status\":\"PENDING\"}";
    private final AtomicInteger falhasRestantes = new AtomicInteger();
    private final HttpServer server;

    public EvolutionFake() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            String corpo = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            recebidas.add(new Recebida(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("apikey"),
                    exchange.getRequestHeaders().getFirst("Content-Type"), corpo));
            int codigo = falhasRestantes.getAndUpdate(n -> n > 0 ? n - 1 : 0) > 0 ? 500 : status;
            byte[] bytes = (codigo >= 400 ? "{\"error\":\"falha\"}" : resposta).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(codigo, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    /** As proximas N chamadas respondem 500; depois volta ao status normal. */
    public void falharProximas(int vezes) {
        falhasRestantes.set(vezes);
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public List<Recebida> lista() {
        return new CopyOnWriteArrayList<>(recebidas);
    }

    public void parar() {
        server.stop(0);
    }
}
