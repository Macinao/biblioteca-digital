package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.enums.StatusReserva;
import com.digital.biblioteca.eventlistener.DevolverEvent;
import com.digital.biblioteca.eventlistener.LivroAtualizarEvent;
import com.digital.biblioteca.eventlistener.ReservarEvent;
import com.digital.biblioteca.mapper.LivroMapper;
import com.digital.biblioteca.mapper.ReservaMapper;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.LivroRepository;
import com.digital.biblioteca.repository.ReservaDocumentRepository;
import com.digital.biblioteca.repository.ReservaRepository;
import com.digital.biblioteca.repository.UsuarioRepository;
import com.digital.biblioteca.request.DevolverRequest;
import com.digital.biblioteca.request.ReservarRequest;
import com.digital.biblioteca.response.ReservaResponse;
import com.digital.biblioteca.service.ReservaService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ReservaServiceImpl implements ReservaService {

    public static final long LIMITE_RESERVAS_POR_USUARIO = 3L;

    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final ReservaRepository reservaRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ReservaMapper reservaMapper;
    private final ReservaDocumentRepository reservaDocumentRepository;
    private final LivroMapper livroMapper;

    @Override
    public ReservaResponse reservar(ReservarRequest request) {

        Usuario usuario = usuarioRepository.findByMatricula(request.getUsuario())
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

        eventPublisher.publishEvent(new ReservarEvent(
                reservaMapper.reservaParaDocument(novaReserva)));

        eventPublisher.publishEvent(new LivroAtualizarEvent(
                livroMapper.livroParaDocument(livro)));

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

        eventPublisher.publishEvent(new DevolverEvent(
                reservaMapper.reservaParaDocument(devolver)));

        eventPublisher.publishEvent(new LivroAtualizarEvent(
                livroMapper.livroParaDocument(livro)));

    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listar() {
        return reservaMapper.listaDocumentParaResponse(
                reservaDocumentRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservaResponse> listarPorUsuario(Usuario usuario) {

        return reservaMapper.listaDocumentParaResponse(
                reservaDocumentRepository
                        .findByMatriculaUsuario(usuario.getMatricula()));

    }
}
