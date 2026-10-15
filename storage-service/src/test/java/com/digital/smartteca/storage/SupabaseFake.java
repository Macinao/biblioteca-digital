package com.digital.smartteca.storage;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Imita o Supabase Storage: guarda os arquivos em memoria por caminho e grava cada requisicao recebida. */
public final class SupabaseFake {

    public record Recebida(String metodo, String caminho, String authorization, String apikey,
                           String upsert, String contentType, byte[] corpo) {
    }

    public final ConcurrentLinkedQueue<Recebida> recebidas = new ConcurrentLinkedQueue<>();
    public final Map<String, byte[]> arquivos = new ConcurrentHashMap<>();
    public final Map<String, String> tiposDeConteudo = new ConcurrentHashMap<>();
    /** Quando diferente de 0, toda requisicao responde com esse status. */
    public volatile int statusForcado = 0;
    private final HttpServer server;

    public SupabaseFake() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            String caminho = exchange.getRequestURI().getRawPath();
            byte[] corpo = exchange.getRequestBody().readAllBytes();
            var h = exchange.getRequestHeaders();
            recebidas.add(new Recebida(exchange.getRequestMethod(), caminho, h.getFirst("Authorization"),
                    h.getFirst("apikey"), h.getFirst("x-upsert"), h.getFirst("Content-Type"), corpo));

            int status;
            byte[] resposta = new byte[0];
            if (statusForcado != 0) {
                status = statusForcado;
            } else {
                switch (exchange.getRequestMethod()) {
                    case "POST" -> {
                        arquivos.put(caminho, corpo);
                        tiposDeConteudo.put(caminho, h.getFirst("Content-Type"));
                        status = 200;
                    }
                    case "GET" -> {
                        resposta = arquivos.getOrDefault(caminho, null);
                        if (resposta == null) {
                            status = 404;
                            resposta = "{\"error\":\"not_found\"}".getBytes(StandardCharsets.UTF_8);
                        } else {
                            status = 200;
                            exchange.getResponseHeaders().add("Content-Type",
                                    tiposDeConteudo.getOrDefault(caminho, "application/octet-stream"));
                        }
                    }
                    case "DELETE" -> status = arquivos.remove(caminho) != null ? 200 : 404;
                    default -> status = 405;
                }
            }
            exchange.sendResponseHeaders(status, resposta.length == 0 ? -1 : resposta.length);
            if (resposta.length > 0) {
                exchange.getResponseBody().write(resposta);
            }
            exchange.close();
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void parar() {
        server.stop(0);
    }

    public void limpar() {
        recebidas.clear();
        arquivos.clear();
        tiposDeConteudo.clear();
        statusForcado = 0;
    }
}
