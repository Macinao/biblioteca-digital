package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.eventlistener.*;
import com.digital.biblioteca.mapper.LivroMapper;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarLivroRequest;
import com.digital.biblioteca.request.CadastrarLivroRequest;
import com.digital.biblioteca.response.LivroResponse;
import com.digital.biblioteca.service.LivroService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LivroServiceImpl implements LivroService {

    private final LivroRepository livroRepository;
    private final LivroMapper livroMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;
    private final LivroDocumentRepository livroDocumentRepository;
    private final ReservaRepository reservaRepository;

    @Override
    public LivroResponse cadastrar(CadastrarLivroRequest request, MultipartFile capa) {

        Livro novoLivro = livroRepository.save(
                livroMapper.cadastroRequestParaLivro(request));

        novoLivro.setTemas(temaRepository.findAllById(request.getTemas()));

        novoLivro.setCursos(cursoRepository.findAllById(request.getCursos()));

        novoLivro.setCapa(novoLivro.getId().toString());

        eventPublisher.publishEvent(new SubirEvent(capa,
                BucketSupabase.LIVRO_CAPA, novoLivro.getCapa()));

        eventPublisher.publishEvent(new LivroCadastroEvent(
                livroMapper.livroParaDocument(novoLivro)));

        return livroMapper.livroParaResponse(novoLivro);

    }

    @Override
    public LivroResponse atualizar(AtualizarLivroRequest request, MultipartFile capa) {

        Livro livroAtualizado = livroRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        livroAtualizado.setTemas(temaRepository.findAllById(request.getTemas()));

        livroAtualizado.setCursos(cursoRepository.findAllById(request.getCursos()));

        livroMapper.atualizarRequestParaLivro(request, livroAtualizado);

        if (capa != null && !capa.isEmpty()) {
            eventPublisher.publishEvent(new SubirEvent(capa,
                    BucketSupabase.LIVRO_CAPA, livroAtualizado.getCapa()));
        }

        livroRepository.save(livroAtualizado);

        eventPublisher.publishEvent(new LivroAtualizarEvent(
                livroMapper.livroParaDocument(livroAtualizado)));

        return livroMapper.livroParaResponse(livroAtualizado);

    }

    @Override
    public List<LivroResponse> listar() {

        return livroMapper.listaDocumentParaResponse(
                livroDocumentRepository.findAll());

    }

    @Override
    public void deletar(UUID id) {

        Livro livroDeletar = livroRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if (reservaRepository.existsByLivroIdAndAtivoTrue(livroDeletar.getId())) {
            throw new RegraNegocioException("Não é possível deletar um livro que possui reservas ativas.");
        }

        livroDeletar.setAtivo(false);

        livroRepository.save(livroDeletar);

        eventPublisher.publishEvent(new LivroDeletarEvent(
                livroMapper.livroParaDocument(livroDeletar)));

        eventPublisher.publishEvent(new DeletarArquivoEvent(
                livroDeletar.getCapa(), BucketSupabase.LIVRO_CAPA));

    }
}
