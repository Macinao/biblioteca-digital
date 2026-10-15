package com.digital.biblioteca.scheduled;

import com.digital.biblioteca.mapper.ReservaMapper;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.ReservaRepository;
import com.digital.smartteca.common.commands.EnviarWhatsappCommand;
import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.smartteca.common.observability.CorrelationId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NotificarAtrasoTest {

    final ReservaRepository reservaRepository = mock(ReservaRepository.class);
    final ReservaMapper reservaMapper = mock(ReservaMapper.class);
    final OutboxService outboxService = mock(OutboxService.class);
    final NotificarAtraso job = new NotificarAtraso(reservaRepository, reservaMapper, outboxService);

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    private static Reserva reserva(String nome, String telefone, String titulo) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setTelefone(telefone);
        Livro livro = new Livro();
        livro.setTitulo(titulo);
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setUsuario(usuario);
        reserva.setLivro(livro);
        reserva.setStatusReserva(StatusReserva.RESERVADO);
        return reserva;
    }

    @Test
    void consultaSoReservasAtivasEmStatusReservadoComPrazoVencido() {
        when(reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(any(), any())).thenReturn(List.of());
        LocalDateTime antes = LocalDateTime.now();

        job.verificarAtraso();

        ArgumentCaptor<LocalDateTime> corte = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(reservaRepository).findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(
                org.mockito.ArgumentMatchers.eq(StatusReserva.RESERVADO), corte.capture());
        assertThat(corte.getValue()).isBetween(antes, LocalDateTime.now());
    }

    @Test
    void semReservasAtrasadasNaoGravaNemPublicaNada() {
        when(reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(any(), any())).thenReturn(List.of());

        job.verificarAtraso();

        verify(reservaRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void marcaComoAtrasadaEPublicaEventoNoKafkaEComandoDeWhatsappNoRabbit() {
        Reserva reserva = reserva("Maria", "83999998888", "Clean Code");
        when(reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(any(), any())).thenReturn(List.of(reserva));

        job.verificarAtraso();

        assertThat(reserva.getStatusReserva()).isEqualTo(StatusReserva.ATRASADO);
        verify(reservaRepository).save(reserva);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> tipo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Broker> broker = ArgumentCaptor.forClass(Broker.class);
        ArgumentCaptor<String> destino = ArgumentCaptor.forClass(String.class);
        verify(outboxService, org.mockito.Mockito.times(2)).record(
                org.mockito.ArgumentMatchers.eq("Reserva"), org.mockito.ArgumentMatchers.eq(reserva.getId().toString()),
                tipo.capture(), payload.capture(), broker.capture(), destino.capture());

        assertThat(tipo.getAllValues()).containsExactly("ReservaAtrasada", "NotificacaoWhatsappSolicitada");
        assertThat(broker.getAllValues()).containsExactly(Broker.KAFKA, Broker.RABBITMQ);
        assertThat(destino.getAllValues()).containsExactly("reserva-events", "notification.whatsapp.send");

        EnviarWhatsappCommand comando = (EnviarWhatsappCommand) payload.getAllValues().get(1);
        assertThat(comando.numero()).isEqualTo("83999998888");
        assertThat(comando.mensagem()).contains("Maria", "Clean Code", "atrasada");
    }

    @Test
    void processaTodasAsReservasDoLoteGerandoUmaMensagemPorReserva() {
        Reserva a = reserva("Ana", "83911111111", "Livro A");
        Reserva b = reserva("Beto", "83922222222", "Livro B");
        when(reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(any(), any())).thenReturn(List.of(a, b));

        job.verificarAtraso();

        assertThat(a.getStatusReserva()).isEqualTo(StatusReserva.ATRASADO);
        assertThat(b.getStatusReserva()).isEqualTo(StatusReserva.ATRASADO);
        verify(outboxService, org.mockito.Mockito.times(4)).record(anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void cadaReservaTemSeuProprioCorrelationIdIgualNosDoisEventosEDiferenteEntreReservas() {
        Reserva a = reserva("Ana", "83911111111", "Livro A");
        Reserva b = reserva("Beto", "83922222222", "Livro B");
        when(reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(any(), any())).thenReturn(List.of(a, b));
        List<String> ids = new ArrayList<>();
        doAnswer(inv -> {
            ids.add(MDC.get(CorrelationId.MDC_KEY));
            return null;
        }).when(outboxService).record(anyString(), anyString(), anyString(), any(), any(), anyString());

        job.verificarAtraso();

        assertThat(ids).hasSize(4).doesNotContainNull();
        assertThat(ids.get(0)).isEqualTo(ids.get(1));
        assertThat(ids.get(2)).isEqualTo(ids.get(3));
        assertThat(ids.get(0)).isNotEqualTo(ids.get(2));
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }
}
