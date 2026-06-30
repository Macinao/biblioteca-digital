package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.config.ai.Mensagem;
import com.digital.biblioteca.document.MemoriaDocument;
import com.digital.biblioteca.repository.MemoriaDocumentRepository;
import com.digital.biblioteca.service.MemoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MemoriaServiceImpl implements MemoriaService {

    private final MemoriaDocumentRepository memoriaDocumentRepository;
    private final ConcurrentHashMap<String, Object> trancas = new ConcurrentHashMap<>();

    @Override
    public void adicionarMensagem(String id, String role, String conteudo) {

        Object tranca = trancas.computeIfAbsent(id, k -> new Object());

        synchronized (tranca) {

            MemoriaDocument memoriaDocument = memoriaDocumentRepository
                    .findById(id)
                    .orElseGet(() -> {

                        MemoriaDocument novaMemoria = new MemoriaDocument();

                        novaMemoria.setId(id);
                        novaMemoria.setConversas(new ArrayList<>());

                        return novaMemoria;
                    });

            memoriaDocument.getConversas()
                    .add(new Mensagem(role, conteudo));

            memoriaDocumentRepository.save(memoriaDocument);

        }

    }

    @Override
    public MemoriaDocument buscar(String id) {

        Object tranca = trancas.computeIfAbsent(id, k -> new Object());

        synchronized (tranca) {

            return memoriaDocumentRepository.findById(id)
                    .orElseGet(() -> {

                        MemoriaDocument memoria = new MemoriaDocument();

                        memoria.setId(id);
                        memoria.setConversas(new ArrayList<>());

                        return memoriaDocumentRepository.save(memoria);
                    });

        }

    }
}
