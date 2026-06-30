package com.digital.biblioteca.scheduled;

import com.digital.biblioteca.component.MetodosMensagem;
import com.digital.biblioteca.enums.StatusReserva;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificarAtraso {

    private final ReservaRepository reservaRepository;
    private final MetodosMensagem metodosMensagem;

    @Scheduled(
            cron = "0 0 0 * * *",
            zone = "America/Sao_Paulo"
    )
    public void verificarAtraso() {

        List<Reserva> reservas = reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(
                StatusReserva.RESERVADO, LocalDateTime.now());

        for (Reserva reserva : reservas) {
            reserva.setStatusReserva(StatusReserva.ATRASADO);
            reservaRepository.save(reserva);
        }

    }

    @Scheduled(
            initialDelay = 30000, fixedDelay = Long.MAX_VALUE
    )
    public void notificarAtraso() {

        List<Reserva> reservas = reservaRepository.findByAtivoTrueAndStatusReserva(StatusReserva.ATRASADO);

        for (Reserva reserva : reservas) {
            metodosMensagem.enviarMensagem(reserva.getUsuario().getTelefone(),
                    "Olá, " + reserva.getUsuario().getNome()
                            + "! Informamos que a devolução do livro '" +
                            reserva.getLivro().getTitulo()
                            + "' está atrasada. Por favor, devolva o livro o mais breve possível para evitar multas.");
        }

    }

}
