package com.digital.smartteca.chatbot.service;

import com.digital.smartteca.chatbot.ai.SmartBot;
import com.digital.smartteca.chatbot.memoria.MemoriaService;
import com.digital.smartteca.chatbot.memoria.Mensagem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotServiceImplTest {

    final SmartBot bot = mock(SmartBot.class);
    final MemoriaService memoria = mock(MemoriaService.class);
    final ChatbotServiceImpl service = new ChatbotServiceImpl(bot, memoria);

    private static Mensagem msg(String role, String conteudo) {
        return new Mensagem(role, conteudo);
    }

    // ---------- historico enviado ao modelo ----------

    @Test
    void semConversaAnteriorMandaOTextoPadraoDeHistoricoVazio() {
        when(memoria.buscarUltimasMensagens("2023001", 10)).thenReturn(List.of());
        when(bot.chat(anyString(), anyString(), anyString())).thenReturn("resposta");

        service.chatbotConversa("Tem livro de Java?", "2023001");

        ArgumentCaptor<String> historico = ArgumentCaptor.forClass(String.class);
        verify(bot).chat(org.mockito.ArgumentMatchers.eq("2023001"), historico.capture(), org.mockito.ArgumentMatchers.eq("Tem livro de Java?"));
        assertThat(historico.getValue()).contains("Nenhuma conversa anterior");
    }

    @Test
    void comConversaAnteriorMontaOHistoricoEmLinhasRoleDoisPontosConteudo() {
        when(memoria.buscarUltimasMensagens("2023001", 10))
                .thenReturn(List.of(msg("usuario", "oi"), msg("assistente", "olá")));
        when(bot.chat(anyString(), anyString(), anyString())).thenReturn("resposta");

        service.chatbotConversa("e TCC?", "2023001");

        ArgumentCaptor<String> historico = ArgumentCaptor.forClass(String.class);
        verify(bot).chat(anyString(), historico.capture(), anyString());
        assertThat(historico.getValue()).isEqualTo("usuario: oi\nassistente: olá");
    }

    @Test
    void usaAsDezUltimasMensagensComoContexto() {
        when(memoria.buscarUltimasMensagens("2023001", 10)).thenReturn(List.of());
        when(bot.chat(anyString(), anyString(), anyString())).thenReturn("r");

        service.chatbotConversa("x", "2023001");

        verify(memoria).buscarUltimasMensagens("2023001", 10);
    }

    // ---------- persistencia ----------

    @Test
    void devolveARespostaEGuardaAPerguntaEARespostaNessaOrdemDepoisDeResponder() {
        when(memoria.buscarUltimasMensagens(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(bot.chat("2023001", "usuario: Nenhuma conversa anterior. \n assistente: Nenhuma conversa anterior.", "Quero um TCC"))
                .thenReturn("Encontrei este TCC");

        String resposta = service.chatbotConversa("Quero um TCC", "2023001");

        assertThat(resposta).isEqualTo("Encontrei este TCC");
        InOrder ordem = inOrder(bot, memoria);
        ordem.verify(bot).chat(anyString(), anyString(), anyString());
        ordem.verify(memoria).adicionarMensagem("2023001", "usuario", "Quero um TCC");
        ordem.verify(memoria).adicionarMensagem("2023001", "assistente", "Encontrei este TCC");
    }

    // ---------- retry de tool_use_failed ----------

    @Test
    void toolUseFailedUmaVezTentaDeNovoEDevolveARespostaDaSegundaTentativa() {
        when(memoria.buscarUltimasMensagens(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(bot.chat(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("400 tool_use_failed: invalid function call"))
                .thenReturn("Agora deu certo");

        String resposta = service.chatbotConversa("busca", "2023001");

        assertThat(resposta).isEqualTo("Agora deu certo");
        verify(bot, times(2)).chat(anyString(), anyString(), anyString());
    }

    @Test
    void toolUseFailedNasDuasTentativasDevolveMensagemAmigavelEGuardaNaMemoria() {
        when(memoria.buscarUltimasMensagens(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(bot.chat(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("tool_use_failed"));

        String resposta = service.chatbotConversa("busca", "2023001");

        assertThat(resposta).startsWith("Desculpe, tive um problema técnico");
        verify(bot, times(2)).chat(anyString(), anyString(), anyString());
        verify(memoria).adicionarMensagem("2023001", "assistente", resposta);
    }

    @Test
    void outroTipoDeErroNaoFazRetryPropagaENaoGuardaNada() {
        when(memoria.buscarUltimasMensagens(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(bot.chat(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.chatbotConversa("busca", "2023001"))
                .isInstanceOf(RuntimeException.class).hasMessage("connection refused");

        verify(bot, times(1)).chat(anyString(), anyString(), anyString());
        verify(memoria, never()).adicionarMensagem(anyString(), anyString(), anyString());
    }

    @Test
    void erroSemMensagemNaoQuebraATratamentoDoRetry() {
        when(memoria.buscarUltimasMensagens(anyString(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of());
        when(bot.chat(anyString(), anyString(), anyString())).thenThrow(new RuntimeException((String) null));

        assertThatThrownBy(() -> service.chatbotConversa("busca", "2023001")).isInstanceOf(RuntimeException.class);

        verify(bot, times(1)).chat(anyString(), anyString(), anyString());
    }
}
