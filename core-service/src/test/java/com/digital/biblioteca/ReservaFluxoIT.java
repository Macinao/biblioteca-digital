package com.digital.biblioteca;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.ReservaRepository;
import com.digital.biblioteca.request.DevolverRequest;
import com.digital.biblioteca.request.ReservarRequest;
import com.digital.biblioteca.response.ReservaResponse;
import com.digital.biblioteca.scheduled.NotificarAtraso;
import com.digital.biblioteca.service.implement.ReservaServiceImpl;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import com.digital.smartteca.common.enums.StatusReserva;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reservar, devolver e o job de atraso, com banco real e outbox real. */
class ReservaFluxoIT extends IntegracaoBase {

    @Autowired ReservaServiceImpl reservaService;
    @Autowired ReservaRepository reservaRepository;
    @Autowired NotificarAtraso notificarAtraso;
    @Autowired ObjectMapper objectMapper;

    private static ReservarRequest pedido(String matricula, UUID livroId) {
        ReservarRequest request = new ReservarRequest();
        request.setUsuario(matricula);
        request.setLivro(livroId);
        return request;
    }

    private List<Map<String, Object>> outbox() {
        return jdbc.queryForList("SELECT aggregate_type, aggregate_id, event_type, broker, destination, status, payload "
                + "FROM outbox_event ORDER BY data_criacao, event_type");
    }

    private int quantidade(UUID livroId) {
        return livroRepository.findById(livroId).orElseThrow().getQuantidade();
    }

    // ---------- reservar ----------

    @Test
    void reservarCriaAReservaBaixaOEstoqueEGravaDoisEventosPendentesNoOutbox() throws Exception {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 2);
        LocalDateTime antes = LocalDateTime.now();

        ReservaResponse resposta = reservaService.reservar(pedido("2023001", livro.getId()));

        assertThat(resposta.getStatusReserva()).isEqualTo(StatusReserva.RESERVADO);
        assertThat(resposta.getTituloLivro()).isEqualTo("Clean Code");
        assertThat(quantidade(livro.getId())).isEqualTo(1);

        Reserva salva = reservaRepository.findById(resposta.getId()).orElseThrow();
        assertThat(salva.isAtivo()).isTrue();
        assertThat(salva.getStatusReserva()).isEqualTo(StatusReserva.RESERVADO);
        assertThat(salva.getDataDevolucao()).isBetween(antes.plusDays(7), LocalDateTime.now().plusDays(7));

        List<Map<String, Object>> eventos = outbox();
        assertThat(eventos).extracting(e -> e.get("event_type")).containsExactlyInAnyOrder("ReservaCriada", "LivroAtualizado");
        assertThat(eventos).allSatisfy(e -> {
            assertThat(e.get("broker")).isEqualTo("KAFKA");
            assertThat(e.get("status")).isEqualTo("PENDING");
        });
        Map<String, Object> eventoReserva = eventos.stream().filter(e -> e.get("event_type").equals("ReservaCriada")).findFirst().orElseThrow();
        assertThat(eventoReserva.get("destination")).isEqualTo("reserva-events");
        assertThat(eventoReserva.get("aggregate_id")).isEqualTo(resposta.getId().toString());
        JsonNode payload = objectMapper.readTree((String) eventoReserva.get("payload"));
        assertThat(payload.get("matriculaUsuario").asText()).isEqualTo("2023001");
        assertThat(payload.get("tituloLivro").asText()).isEqualTo("Clean Code");
        assertThat(payload.get("livroId").asText()).isEqualTo(livro.getId().toString());
    }

    @Test
    void usuarioPodeTerAteTresReservasAtivasEAQuartaEBloqueadaSemMexerNoEstoque() {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 10);

        for (int i = 0; i < 3; i++) {
            reservaService.reservar(pedido("2023001", livro.getId()));
        }

        assertThatThrownBy(() -> reservaService.reservar(pedido("2023001", livro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Usuário atingiu o limite de 3 reservas ativas.");
        assertThat(quantidade(livro.getId())).isEqualTo(7);
        assertThat(reservaRepository.count()).isEqualTo(3);
    }

    @Test
    void devolverLiberaVagaNoLimiteDoUsuario() {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 10);
        ReservaResponse primeira = reservaService.reservar(pedido("2023001", livro.getId()));
        reservaService.reservar(pedido("2023001", livro.getId()));
        reservaService.reservar(pedido("2023001", livro.getId()));

        DevolverRequest devolucao = new DevolverRequest();
        devolucao.setReserva(primeira.getId());
        reservaService.devolver(devolucao);

        assertThat(reservaService.reservar(pedido("2023001", livro.getId())).getId()).isNotNull();
    }

    @Test
    void reservarSemEstoqueFalhaSemCriarReservaNemEventos() {
        criarUsuario("2023001", Role.ALUNO);
        Livro esgotado = criarLivro("Esgotado", 0);

        assertThatThrownBy(() -> reservaService.reservar(pedido("2023001", esgotado.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não há exemplares disponíveis para reserva.");

        assertThat(reservaRepository.count()).isZero();
        assertThat(outbox()).isEmpty();
        assertThat(quantidade(esgotado.getId())).isZero();
    }

    @Test
    void reservarLivroInativoFalhaComoNaoEncontradoESemEfeitos() {
        criarUsuario("2023001", Role.ALUNO);
        Livro inativo = criarLivro("Inativo", 5);
        inativo.setAtivo(false);
        livroRepository.save(inativo);

        assertThatThrownBy(() -> reservaService.reservar(pedido("2023001", inativo.getId())))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(reservaRepository.count()).isZero();
        assertThat(quantidade(inativo.getId())).isEqualTo(5);
    }

    @Test
    void reservarComUsuarioInexistenteFalha() {
        Livro livro = criarLivro("Clean Code", 2);

        assertThatThrownBy(() -> reservaService.reservar(pedido("0000000", livro.getId())))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(quantidade(livro.getId())).isEqualTo(2);
    }

    // ---------- devolver ----------

    @Test
    void devolverEncerraAReservaRepoeOEstoqueEGravaEventos() {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 1);
        ReservaResponse reservada = reservaService.reservar(pedido("2023001", livro.getId()));
        jdbc.update("DELETE FROM outbox_event");
        DevolverRequest devolucao = new DevolverRequest();
        devolucao.setReserva(reservada.getId());

        reservaService.devolver(devolucao);

        assertThat(reservaRepository.findById(reservada.getId()).orElseThrow().isAtivo()).isFalse();
        assertThat(quantidade(livro.getId())).isEqualTo(1);
        assertThat(outbox()).extracting(e -> e.get("event_type")).containsExactlyInAnyOrder("ReservaDevolvida", "LivroAtualizado");
    }

    @Test
    void devolverDuasVezesAMesmaReservaFalhaNaSegundaENaoInflaOEstoque() {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 1);
        ReservaResponse reservada = reservaService.reservar(pedido("2023001", livro.getId()));
        DevolverRequest devolucao = new DevolverRequest();
        devolucao.setReserva(reservada.getId());
        reservaService.devolver(devolucao);

        assertThatThrownBy(() -> reservaService.devolver(devolucao)).isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(quantidade(livro.getId())).isEqualTo(1);
    }

    // ---------- concorrencia ----------

    @Test
    void vinteReservasSimultaneasDoUltimoExemplarNuncaDeixamOEstoqueNegativo() throws Exception {
        int threads = 20;
        Livro livro = criarLivro("Ultimo exemplar", 1);
        List<String> matriculas = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            String matricula = String.format("C%06d", i);
            criarUsuario(matricula, Role.ALUNO);
            matriculas.add(matricula);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch prontos = new CountDownLatch(threads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhas = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (String matricula : matriculas) {
            futures.add(executor.submit(() -> {
                prontos.countDown();
                try {
                    largada.await();
                    reservaService.reservar(pedido(matricula, livro.getId()));
                    sucessos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    falhas.incrementAndGet();
                }
            }));
        }
        prontos.await();
        largada.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(sucessos.get()).as("reservas com sucesso").isEqualTo(1);
        assertThat(falhas.get()).as("reservas recusadas").isEqualTo(threads - 1);
        assertThat(quantidade(livro.getId())).as("estoque final").isZero();
        assertThat(reservaRepository.count()).as("reservas gravadas").isEqualTo(1);
    }

    @Test
    void dezReservasSimultaneasDoMesmoUsuarioNuncaPassamDoLimiteDeTres() throws Exception {
        int threads = 10;
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Muitos exemplares", 50);

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch prontos = new CountDownLatch(threads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                prontos.countDown();
                try {
                    largada.await();
                    reservaService.reservar(pedido("2023001", livro.getId()));
                    sucessos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // recusada pelo limite: esperado para as demais
                }
            }));
        }
        prontos.await();
        largada.countDown();
        for (Future<?> future : futures) {
            future.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(sucessos.get()).as("reservas aceitas").isEqualTo(3);
        assertThat(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).as("reservas ativas").isEqualTo(3);
        assertThat(quantidade(livro.getId())).as("estoque final").isEqualTo(47);
    }

    // ---------- job de atraso ----------

    @Test
    void jobDeAtrasoMarcaReservaVencidaEGravaEventoKafkaEComandoDeWhatsapp() throws Exception {
        Usuario maria = criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 3);
        ReservaResponse reservada = reservaService.reservar(pedido("2023001", livro.getId()));
        jdbc.update("UPDATE reserva SET data_devolucao = ? WHERE id = ?", LocalDateTime.now().minusDays(1), reservada.getId());
        jdbc.update("DELETE FROM outbox_event");

        notificarAtraso.verificarAtraso();

        assertThat(reservaRepository.findById(reservada.getId()).orElseThrow().getStatusReserva()).isEqualTo(StatusReserva.ATRASADO);
        List<Map<String, Object>> eventos = outbox();
        assertThat(eventos).extracting(e -> e.get("event_type")).containsExactlyInAnyOrder("ReservaAtrasada", "NotificacaoWhatsappSolicitada");
        Map<String, Object> whatsapp = eventos.stream().filter(e -> e.get("event_type").equals("NotificacaoWhatsappSolicitada")).findFirst().orElseThrow();
        assertThat(whatsapp.get("broker")).isEqualTo("RABBITMQ");
        assertThat(whatsapp.get("destination")).isEqualTo("notification.whatsapp.send");
        JsonNode comando = objectMapper.readTree((String) whatsapp.get("payload"));
        assertThat(comando.get("numero").asText()).isEqualTo(maria.getTelefone());
        assertThat(comando.get("mensagem").asText()).contains("Usuario 2023001", "Clean Code");
    }

    @Test
    void jobDeAtrasoNaoReprocessaReservaJaAtrasadaNemMexeEmReservaNoPrazo() {
        criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 5);
        ReservaResponse vencida = reservaService.reservar(pedido("2023001", livro.getId()));
        ReservaResponse noPrazo = reservaService.reservar(pedido("2023001", livro.getId()));
        jdbc.update("UPDATE reserva SET data_devolucao = ? WHERE id = ?", LocalDateTime.now().minusDays(1), vencida.getId());
        jdbc.update("DELETE FROM outbox_event");

        notificarAtraso.verificarAtraso();
        int eventosAposPrimeiraRodada = outbox().size();
        notificarAtraso.verificarAtraso();

        assertThat(eventosAposPrimeiraRodada).isEqualTo(2);
        assertThat(outbox()).hasSize(2);
        assertThat(reservaRepository.findById(noPrazo.getId()).orElseThrow().getStatusReserva()).isEqualTo(StatusReserva.RESERVADO);
    }
}
