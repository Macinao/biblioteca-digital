package com.digital.smartteca.chatbot.memoria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class MemoriaServiceImplTest {

    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    final ListOperations<String, String> lista = mock(ListOperations.class);
    final ObjectMapper objectMapper = new ObjectMapper();
    final MemoriaServiceImpl service;

    MemoriaServiceImplTest() {
        when(redis.opsForList()).thenReturn(lista);
        service = new MemoriaServiceImpl(redis, objectMapper);
    }

    @Test
    void adicionarFazRpushDoJsonNaChaveDaMatriculaEDepoisPodaParaAsUltimasCinquenta() throws Exception {
        service.adicionarMensagem("2023001", "usuario", "Tem livro de Java?");

        var ordem = inOrder(lista);
        ordem.verify(lista).rightPush("chatbot:memoria:2023001", "{\"role\":\"usuario\",\"conteudo\":\"Tem livro de Java?\"}");
        ordem.verify(lista).trim("chatbot:memoria:2023001", -50, -1);
    }

    @Test
    void conteudoComAspasEQuebrasDeLinhaEscapadoCorretamenteNoJson() throws Exception {
        service.adicionarMensagem("2023001", "assistente", "Ele disse \"oi\"\ne saiu");

        var captor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(lista).rightPush(org.mockito.ArgumentMatchers.eq("chatbot:memoria:2023001"), captor.capture());
        Mensagem lida = objectMapper.readValue(captor.getValue(), Mensagem.class);
        assertThat(lida.getConteudo()).isEqualTo("Ele disse \"oi\"\ne saiu");
        assertThat(lida.getRole()).isEqualTo("assistente");
    }

    @Test
    void buscarLeAsUltimasNMensagensDaChaveDaMatricula() {
        when(lista.range("chatbot:memoria:2023001", -10, -1)).thenReturn(List.of(
                "{\"role\":\"usuario\",\"conteudo\":\"oi\"}", "{\"role\":\"assistente\",\"conteudo\":\"olá\"}"));

        List<Mensagem> mensagens = service.buscarUltimasMensagens("2023001", 10);

        assertThat(mensagens).extracting(Mensagem::getRole, Mensagem::getConteudo)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("usuario", "oi"), org.assertj.core.groups.Tuple.tuple("assistente", "olá"));
    }

    @Test
    void semHistoricoOuRetornoNuloDevolveListaVazia() {
        when(lista.range("chatbot:memoria:novo", -10, -1)).thenReturn(null);
        when(lista.range("chatbot:memoria:vazio", -10, -1)).thenReturn(List.of());

        assertThat(service.buscarUltimasMensagens("novo", 10)).isEmpty();
        assertThat(service.buscarUltimasMensagens("vazio", 10)).isEmpty();
    }

    @Test
    void jsonCorrompidoNoRedisFalhaEmVezDeIgnorarEmSilencio() {
        when(lista.range("chatbot:memoria:2023001", -10, -1)).thenReturn(List.of("isto nao e json"));

        assertThatThrownBy(() -> service.buscarUltimasMensagens("2023001", 10))
                .isInstanceOf(com.fasterxml.jackson.core.JsonProcessingException.class);
    }

    @Test
    void cadaMatriculaTemSuaPropriaChave() {
        service.adicionarMensagem("A", "usuario", "x");
        service.adicionarMensagem("B", "usuario", "y");

        verify(lista).rightPush(org.mockito.ArgumentMatchers.eq("chatbot:memoria:A"), org.mockito.ArgumentMatchers.anyString());
        verify(lista).rightPush(org.mockito.ArgumentMatchers.eq("chatbot:memoria:B"), org.mockito.ArgumentMatchers.anyString());
    }
}
