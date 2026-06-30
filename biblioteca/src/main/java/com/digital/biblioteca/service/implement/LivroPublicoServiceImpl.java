package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.eventlistener.*;
import com.digital.biblioteca.mapper.LivroPublicoMapper;
import com.digital.biblioteca.model.LivroPublico;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.response.LivroPublicoResponse;
import com.digital.biblioteca.service.LivroPublicoService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
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
public class LivroPublicoServiceImpl implements LivroPublicoService {

    private final LivroPublicoRepository livroPublicoRepository;
    private final LivroPublicoMapper livroPublicoMapper;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;

    @Override
    public LivroPublicoResponse cadastrar(CadastrarLivroPublicoRequest request,
                                          MultipartFile conteudo, MultipartFile capa) {

        LivroPublico novoLivro = livroPublicoRepository.save(
                livroPublicoMapper.cadastroRequestParaLivroPublico(request));

        novoLivro.setTemas(temaRepository.findAllById(request.getTemas()));

        novoLivro.setCursos(cursoRepository.findAllById(request.getCursos()));

        novoLivro.setCapa(novoLivro.getId().toString());

        novoLivro.setConteudo(novoLivro.getId().toString());

        eventPublisher.publishEvent(new SubirEvent(conteudo,
                BucketSupabase.LIVRO_PUBLICO_CONTEUDO, novoLivro.getConteudo()));

        eventPublisher.publishEvent(new SubirEvent(capa,
                BucketSupabase.LIVRO_PUBLICO_CAPA, novoLivro.getCapa()));

        eventPublisher.publishEvent(new LivroPublicoCadastroEvent(novoLivro));

        return livroPublicoMapper.livroPublicoParaResponse(novoLivro);

    }

    @Override
    public LivroPublicoResponse atualizar(AtualizarLivroPublicoRequest request, MultipartFile conteudo, MultipartFile capa) {

        LivroPublico livroAtualizado = livroPublicoRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro público não encontrado"));

        livroAtualizado.setTemas(temaRepository.findAllById(request.getTemas()));

        livroAtualizado.setCursos(cursoRepository.findAllById(request.getCursos()));

        livroPublicoMapper.atualizarRequestParaLivroPublico(request, livroAtualizado);

        if (conteudo != null && !conteudo.isEmpty()) {
            eventPublisher.publishEvent(new SubirEvent(conteudo,
                    BucketSupabase.LIVRO_PUBLICO_CONTEUDO, livroAtualizado.getConteudo()));
        }

        if (capa != null && !capa.isEmpty()) {
            eventPublisher.publishEvent(new SubirEvent(capa,
                    BucketSupabase.LIVRO_PUBLICO_CAPA, livroAtualizado.getCapa()));
        }

        livroPublicoRepository.save(livroAtualizado);

        eventPublisher.publishEvent(new LivroPublicoAtualizarEvent(
                livroPublicoMapper.livroPublicoParaDocument(livroAtualizado)));

        return livroPublicoMapper.livroPublicoParaResponse(livroAtualizado);

    }

    @Override
    public List<LivroPublicoResponse> listar() {

        return livroPublicoMapper.listaDocumentParaResponse(
                livroPublicoDocumentRepository.findAll());

    }

    @Override
    public void deletar(UUID id) {

        LivroPublico livroDeletar = livroPublicoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro público não encontrado"));

        livroDeletar.setAtivo(false);

        livroPublicoRepository.save(livroDeletar);

        eventPublisher.publishEvent(new LivroPublicoDeletarEvent(
                livroPublicoMapper.livroPublicoParaDocument(livroDeletar)));

        eventPublisher.publishEvent(new DeletarContudoCapaEvent(livroDeletar.getConteudo(),
                livroDeletar.getCapa(), BucketSupabase.LIVRO_PUBLICO_CONTEUDO,
                BucketSupabase.LIVRO_PUBLICO_CAPA));

    }
}
