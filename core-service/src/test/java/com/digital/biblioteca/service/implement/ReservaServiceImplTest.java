package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.mapper.LivroMapper;
import com.digital.biblioteca.mapper.ReservaMapper;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.LivroRepository;
import com.digital.biblioteca.repository.ReservaRepository;
import com.digital.biblioteca.repository.UsuarioRepository;
import com.digital.biblioteca.request.DevolverRequest;
import com.digital.biblioteca.request.ReservarRequest;
import com.digital.biblioteca.response.ReservaResponse;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import com.digital.smartteca.common.enums.StatusReserva;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    @Mock LivroRepository livroRepository;
    @Mock UsuarioRepository usuarioRepository;
    @Mock ReservaRepository reservaRepository;
    @Mock OutboxService outboxService;
    @Mock ReservaMapper reservaMapper;
    @Mock LivroMapper livroMapper;
    @InjectMocks ReservaServiceImpl service;

    private static Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setMatricula("2023001");
        usuario.setNome("Maria Souza");
        return usuario;
    }

    private static Livro livro() {
        Livro livro = new Livro();
        livro.setId(UUID.randomUUID());
        livro.setTitulo("Clean Code");
        livro.setAutor("Robert Martin");
        livro.setQuantidade(2);
        return livro;
    }

    private static ReservarRequest pedido(String matricula, UUID livroId) {
        ReservarRequest request = new ReservarRequest();
        request.setUsuario(matricula);
        request.setLivro(livroId);
        return request;
    }

    private void salvarAtribuindoId() {
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva reserva = inv.getArgument(0);
            reserva.setId(UUID.randomUUID());
            reserva.setDataCriacao(LocalDateTime.now());
            return reserva;
        });
    }

    // ---------- reservar ----------

    @Test
    void reservarFalhaQuandoOUsuarioNaoExiste() {
        when(usuarioRepository.findWithLockByMatricula("0000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservar(pedido("0000000", UUID.randomUUID())))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("0000000");

        verifyNoInteractions(livroRepository, reservaRepository, outboxService);
    }

    @Test
    void reservarBloqueiaAoAtingirOLimiteDeTresReservasAtivasSemMexerNoEstoque() {
        Usuario usuario = usuario();
        when(usuarioRepository.findWithLockByMatricula("2023001")).thenReturn(Optional.of(usuario));
        when(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).thenReturn(3L);

        assertThatThrownBy(() -> service.reservar(pedido("2023001", UUID.randomUUID())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Usuário atingiu o limite de 3 reservas ativas.");

        verifyNoInteractions(livroRepository, outboxService);
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservarPermiteATerceiraReservaQuandoOUsuarioTemDuasAtivas() {
        Usuario usuario = usuario();
        Livro livro = livro();
        when(usuarioRepository.findWithLockByMatricula("2023001")).thenReturn(Optional.of(usuario));
        when(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).thenReturn(2L);
        when(livroRepository.decrementarQuantidade(livro.getId())).thenReturn(1);
        when(livroRepository.findById(livro.getId())).thenReturn(Optional.of(livro));
        salvarAtribuindoId();

        ReservaResponse resposta = service.reservar(pedido("2023001", livro.getId()));

        assertThat(resposta.getStatusReserva()).isEqualTo(StatusReserva.RESERVADO);
    }

    @Test
    void reservarSemEstoqueDeLivroExistenteFalhaPorFaltaDeExemplares() {
        Usuario usuario = usuario();
        Livro livro = livro();
        when(usuarioRepository.findWithLockByMatricula("2023001")).thenReturn(Optional.of(usuario));
        when(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).thenReturn(0L);
        when(livroRepository.decrementarQuantidade(livro.getId())).thenReturn(0);
        when(livroRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));

        assertThatThrownBy(() -> service.reservar(pedido("2023001", livro.getId())))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não há exemplares disponíveis para reserva.");

        verify(reservaRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void reservarDeLivroInexistenteOuInativoFalhaComNaoEncontrado() {
        Usuario usuario = usuario();
        UUID livroId = UUID.randomUUID();
        when(usuarioRepository.findWithLockByMatricula("2023001")).thenReturn(Optional.of(usuario));
        when(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).thenReturn(0L);
        when(livroRepository.decrementarQuantidade(livroId)).thenReturn(0);
        when(livroRepository.findByIdAndAtivoTrue(livroId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservar(pedido("2023001", livroId)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(livroId.toString());

        verifyNoInteractions(outboxService);
    }

    @Test
    void reservarComSucessoCriaReservaPublicaDoisEventosEDevolveOResumo() {
        Usuario usuario = usuario();
        Livro livro = livro();
        when(usuarioRepository.findWithLockByMatricula("2023001")).thenReturn(Optional.of(usuario));
        when(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).thenReturn(0L);
        when(livroRepository.decrementarQuantidade(livro.getId())).thenReturn(1);
        when(livroRepository.findById(livro.getId())).thenReturn(Optional.of(livro));
        salvarAtribuindoId();
        LocalDateTime antes = LocalDateTime.now();

        ReservaResponse resposta = service.reservar(pedido("2023001", livro.getId()));

        ArgumentCaptor<Reserva> salva = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(salva.capture());
        assertThat(salva.getValue().getUsuario()).isSameAs(usuario);
        assertThat(salva.getValue().getLivro()).isSameAs(livro);
        assertThat(salva.getValue().getStatusReserva()).isEqualTo(StatusReserva.RESERVADO);
        assertThat(salva.getValue().getDataDevolucao())
                .isBetween(antes.plusDays(7), LocalDateTime.now().plusDays(7));

        assertThat(resposta.getId()).isEqualTo(salva.getValue().getId());
        assertThat(resposta.getLivroId()).isEqualTo(livro.getId());
        assertThat(resposta.getTituloLivro()).isEqualTo("Clean Code");
        assertThat(resposta.getAutorLivro()).isEqualTo("Robert Martin");
        assertThat(resposta.getMatriculaUsuario()).isEqualTo("2023001");
        assertThat(resposta.getNomeUsuario()).isEqualTo("Maria Souza");
        assertThat(resposta.getStatusReserva()).isEqualTo(StatusReserva.RESERVADO);

        assertThat(OutboxCalls.de(outboxService)).extracting(
                        OutboxCalls.Evento::aggregateType, OutboxCalls.Evento::eventType,
                        OutboxCalls.Evento::broker, OutboxCalls.Evento::destination)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Reserva", "ReservaCriada", Broker.KAFKA, "reserva-events"),
                        org.assertj.core.groups.Tuple.tuple("Livro", "LivroAtualizado", Broker.KAFKA, "livro-events"));
    }

    @Test
    void reservarFalhaSeOLivroSumirEntreODecrementoEALeitura() {
        Usuario usuario = usuario();
        UUID livroId = UUID.randomUUID();
        when(usuarioRepository.findWithLockByMatricula("2023001")).thenReturn(Optional.of(usuario));
        when(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).thenReturn(0L);
        when(livroRepository.decrementarQuantidade(livroId)).thenReturn(1);
        when(livroRepository.findById(livroId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservar(pedido("2023001", livroId)))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(reservaRepository, never()).save(any());
    }

    // ---------- devolver ----------

    @Test
    void devolverFalhaQuandoAReservaAtivaNaoExiste() {
        UUID id = UUID.randomUUID();
        DevolverRequest request = new DevolverRequest();
        request.setReserva(id);
        when(reservaRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.devolver(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining(id.toString());

        verifyNoInteractions(livroRepository, outboxService);
    }

    @Test
    void devolverEncerraAReservaRepoeOEstoquePublicaDoisEventos() {
        Livro livro = livro();
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setLivro(livro);
        reserva.setAtivo(true);
        DevolverRequest request = new DevolverRequest();
        request.setReserva(reserva.getId());
        when(reservaRepository.findByIdAndAtivoTrue(reserva.getId())).thenReturn(Optional.of(reserva));
        when(livroRepository.incrementarQuantidade(livro.getId())).thenReturn(1);
        when(livroRepository.findById(livro.getId())).thenReturn(Optional.of(livro));

        service.devolver(request);

        assertThat(reserva.isAtivo()).isFalse();
        verify(reservaRepository).save(reserva);
        verify(livroRepository).incrementarQuantidade(livro.getId());
        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("ReservaDevolvida", "LivroAtualizado");
    }

    @Test
    void devolverFalhaQuandoOEstoqueNaoPodeSerAtualizadoPorLivroInativo() {
        Livro livro = livro();
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setLivro(livro);
        DevolverRequest request = new DevolverRequest();
        request.setReserva(reserva.getId());
        when(reservaRepository.findByIdAndAtivoTrue(reserva.getId())).thenReturn(Optional.of(reserva));
        when(livroRepository.incrementarQuantidade(livro.getId())).thenReturn(0);

        assertThatThrownBy(() -> service.devolver(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("livro inativo");

        verifyNoInteractions(outboxService);
    }

    @Test
    void devolverFalhaSeOLivroSumirDepoisDeRepostoOEstoque() {
        Livro livro = livro();
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setLivro(livro);
        DevolverRequest request = new DevolverRequest();
        request.setReserva(reserva.getId());
        when(reservaRepository.findByIdAndAtivoTrue(reserva.getId())).thenReturn(Optional.of(reserva));
        when(livroRepository.incrementarQuantidade(livro.getId())).thenReturn(1);
        when(livroRepository.findById(livro.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.devolver(request)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
