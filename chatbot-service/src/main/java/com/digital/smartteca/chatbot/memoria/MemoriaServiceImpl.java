package com.digital.smartteca.chatbot.memoria;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * O MemoriaServiceImpl original guardava a conversa inteira num unico
 * documento do Elasticsearch, protegido por um "synchronized" sobre um
 * ConcurrentHashMap<String, Object> — isso so serializa requisicoes
 * concorrentes DENTRO da mesma instancia da JVM. Com duas replicas do
 * servico (ou, aqui, ao migrar pra um servico proprio), duas requisicoes
 * simultaneas do mesmo usuario podiam ler o mesmo documento, cada uma
 * adicionar sua mensagem na copia em memoria, e o segundo save()
 * sobrescrever o primeiro — uma mensagem perdida, sem erro nenhum.
 *
 * A troca pra Redis elimina o lock por completo: RPUSH e uma operacao
 * atomica no proprio servidor Redis (o Redis processa comandos numa
 * unica thread), entao duas instancias podem chamar adicionarMensagem
 * ao mesmo tempo para o mesmo usuario sem qualquer coordenacao entre
 * elas — nao existe mais "ler, modificar, salvar o documento inteiro".
 * LTRIM depois do RPUSH evita que a lista cresca pra sempre (o original
 * nunca podava o que ficava salvo, so limitava na leitura).
 */
@Service
@RequiredArgsConstructor
public class MemoriaServiceImpl implements MemoriaService {

    private static final int LIMITE_ARMAZENADO = 50;
    private static final String PREFIXO_CHAVE = "chatbot:memoria:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public void adicionarMensagem(String matricula, String role, String conteudo) {

        String chave = chave(matricula);
        String json = objectMapper.writeValueAsString(new Mensagem(role, conteudo));

        redisTemplate.opsForList().rightPush(chave, json);
        redisTemplate.opsForList().trim(chave, -LIMITE_ARMAZENADO, -1);

    }

    @Override
    public List<Mensagem> buscarUltimasMensagens(String matricula, int limite) {

        List<String> valores = redisTemplate.opsForList().range(chave(matricula), -limite, -1);

        if (valores == null || valores.isEmpty()) {
            return List.of();
        }

        return valores.stream().map(this::desserializar).toList();

    }

    @SneakyThrows(JsonProcessingException.class)
    private Mensagem desserializar(String json) {
        return objectMapper.readValue(json, Mensagem.class);
    }

    private String chave(String matricula) {
        return PREFIXO_CHAVE + matricula;
    }

}
