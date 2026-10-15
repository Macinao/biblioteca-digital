package com.digital.biblioteca.scheduled;

import com.digital.biblioteca.mapper.ReservaMapper;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.ReservaRepository;
import com.digital.smartteca.common.commands.EnviarWhatsappCommand;
import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.smartteca.common.observability.CorrelationId;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * O job antigo era dois @Scheduled separados: verificarAtraso() (roda
 * todo dia, funciona) e notificarAtraso() (fixedDelay=Long.MAX_VALUE —
 * roda so uma vez, 30s depois do boot, e nunca mais). Aqui a notificacao
 * deixa de ser um segundo job pra virar consequencia direta do primeiro:
 * assim que uma reserva e marcada ATRASADO, a outbox ja grava o evento
 * (search-service atualiza o status no Elasticsearch) e o comando de
 * WhatsApp (notification-service entrega), na mesma transacao.
 */
@Service
@RequiredArgsConstructor
public class NotificarAtraso {

    private static final String TOPICO_RESERVA = "reserva-events";
    private static final String FILA_WHATSAPP = "notification.whatsapp.send";

    private final ReservaRepository reservaRepository;
    private final ReservaMapper reservaMapper;
    private final OutboxService outboxService;

    @Scheduled(
            cron = "0 0 0 * * *",
            zone = "America/Sao_Paulo"
    )
    @Transactional
    public void verificarAtraso() {

        List<Reserva> reservas = reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(
                StatusReserva.RESERVADO, LocalDateTime.now());

        for (Reserva reserva : reservas) {

            // Um id novo por reserva (nao por execucao do job inteiro): assim
            // o evento Kafka e o comando RabbitMQ desta reserva especifica
            // ficam correlacionaveis entre si, sem misturar com as outras
            // reservas processadas na mesma rodada do cron.
            try (var ignored = CorrelationId.open(null)) {

                reserva.setStatusReserva(StatusReserva.ATRASADO);
                reservaRepository.save(reserva);

                outboxService.record("Reserva", reserva.getId().toString(), "ReservaAtrasada",
                        reservaMapper.reservaParaDocument(reserva), Broker.KAFKA, TOPICO_RESERVA);

                String mensagem = "Olá, " + reserva.getUsuario().getNome()
                        + "! Informamos que a devolução do livro '" + reserva.getLivro().getTitulo()
                        + "' está atrasada. Por favor, devolva o livro o mais breve possível para evitar multas.";

                outboxService.record("Reserva", reserva.getId().toString(), "NotificacaoWhatsappSolicitada",
                        new EnviarWhatsappCommand(reserva.getUsuario().getTelefone(), mensagem),
                        Broker.RABBITMQ, FILA_WHATSAPP);

            }

        }

    }

}
