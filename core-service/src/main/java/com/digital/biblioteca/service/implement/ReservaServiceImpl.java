package com.digital.biblioteca.service.implement;

import com.digital.smartteca.common.enums.StatusReserva;
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
import com.digital.biblioteca.service.ReservaService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    public static final long LIMITE_RESERVAS_POR_USUARIO = 3L;

    private static final String TOPICO_RESERVA = "reserva-events";
    private static final String TOPICO_LIVRO = "livro-events";

    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final OutboxService outboxService;
    private final ReservaMapper reservaMapper;
    private final LivroMapper livroMapper;

    @Override
    public ReservaResponse reservar(ReservarRequest request) {

        Usuario usuario = usuarioRepository.findWithLockByMatricula(request.getUsuario())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado com matrícula: " + request.getUsuario()));

        long ativas = reservaRepository.countByUsuarioMatriculaAndAtivoTrue(usuario.getMatricula());
        if (ativas >= LIMITE_RESERVAS_POR_USUARIO) {
            throw new RegraNegocioException(
                    "Usuário atingiu o limite de " + LIMITE_RESERVAS_POR_USUARIO + " reservas ativas.");
        }

        UUID livroId = request.getLivro();
        int atualizadas = livroRepository.decrementarQuantidade(livroId);
        if (atualizadas == 0) {
            livroRepository.findByIdAndAtivoTrue(livroId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Livro não encontrado com id: " + livroId));
            throw new RegraNegocioException("Não há exemplares disponíveis para reserva.");
        }

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Livro não encontrado após decremento de estoque."));

        Reserva novaReserva = new Reserva();
        novaReserva.setUsuario(usuario);
        novaReserva.setLivro(livro);
        novaReserva.setStatusReserva(StatusReserva.RESERVADO);
        novaReserva.setDataDevolucao(LocalDateTime.now().plusDays(7));

        reservaRepository.save(novaReserva);

        outboxService.record("Reserva", novaReserva.getId().toString(), "ReservaCriada",
                reservaMapper.reservaParaDocument(novaReserva), Broker.KAFKA, TOPICO_RESERVA);

        outboxService.record("Livro", livro.getId().toString(), "LivroAtualizado",
                livroMapper.livroParaDocument(livro), Broker.KAFKA, TOPICO_LIVRO);

        return new ReservaResponse(
                novaReserva.getId(),
                livro.getId(),
                livro.getTitulo(),
                livro.getAutor(),
                usuario.getMatricula(),
                usuario.getNome(),
                novaReserva.getStatusReserva(),
                novaReserva.getDataCriacao(),
                novaReserva.getDataDevolucao()
        );

    }

    @Override
    public void devolver(DevolverRequest request) {

        Reserva devolver = reservaRepository.findByIdAndAtivoTrue(request.getReserva())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Reserva não encontrada com id: " + request.getReserva()));

        UUID livroId = devolver.getLivro().getId();

        devolver.setAtivo(false);
        reservaRepository.save(devolver);

        int atualizadas = livroRepository.incrementarQuantidade(livroId);
        if (atualizadas == 0) {
            throw new RegraNegocioException(
                    "Não foi possível atualizar o estoque do livro devolvido (livro inativo?).");
        }

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Livro da reserva não encontrado após devolução."));

        outboxService.record("Reserva", devolver.getId().toString(), "ReservaDevolvida",
                reservaMapper.reservaParaDocument(devolver), Broker.KAFKA, TOPICO_RESERVA);

        outboxService.record("Livro", livro.getId().toString(), "LivroAtualizado",
                livroMapper.livroParaDocument(livro), Broker.KAFKA, TOPICO_LIVRO);

    }
}
