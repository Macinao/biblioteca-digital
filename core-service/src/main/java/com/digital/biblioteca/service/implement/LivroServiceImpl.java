package com.digital.biblioteca.service.implement;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.biblioteca.mapper.LivroMapper;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarLivroRequest;
import com.digital.biblioteca.request.CadastrarLivroRequest;
import com.digital.biblioteca.response.LivroResponse;
import com.digital.biblioteca.service.LivroService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LivroServiceImpl implements LivroService {

    private static final String TOPICO = "livro-events";
    private static final String FILA_UPLOAD = "storage.upload";
    private static final String FILA_DELETE = "storage.delete";

    private final LivroRepository livroRepository;
    private final LivroMapper livroMapper;
    private final OutboxService outboxService;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;
    private final ReservaRepository reservaRepository;

    @Override
    public LivroResponse cadastrar(CadastrarLivroRequest request, MultipartFile capa) throws IOException {

        Livro novoLivro = livroRepository.save(
                livroMapper.cadastroRequestParaLivro(request));

        novoLivro.setTemas(temaRepository.findAllById(request.getTemas()));

        novoLivro.setCursos(cursoRepository.findAllById(request.getCursos()));

        novoLivro.setCapa(novoLivro.getId().toString());

        outboxService.record("Livro", novoLivro.getId().toString(), "ArquivoParaSubir",
                comandoUpload(capa, BucketSupabase.LIVRO_CAPA, novoLivro.getCapa()),
                Broker.RABBITMQ, FILA_UPLOAD);

        outboxService.record("Livro", novoLivro.getId().toString(), "LivroCadastrado",
                livroMapper.livroParaDocument(novoLivro), Broker.KAFKA, TOPICO);

        return livroMapper.livroParaResponse(novoLivro);

    }

    @Override
    public LivroResponse atualizar(AtualizarLivroRequest request, MultipartFile capa) throws IOException {

        Livro livroAtualizado = livroRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        livroAtualizado.setTemas(temaRepository.findAllById(request.getTemas()));

        livroAtualizado.setCursos(cursoRepository.findAllById(request.getCursos()));

        livroMapper.atualizarRequestParaLivro(request, livroAtualizado);

        if (capa != null && !capa.isEmpty()) {
            outboxService.record("Livro", livroAtualizado.getId().toString(), "ArquivoParaSubir",
                    comandoUpload(capa, BucketSupabase.LIVRO_CAPA, livroAtualizado.getCapa()),
                    Broker.RABBITMQ, FILA_UPLOAD);
        }

        livroRepository.save(livroAtualizado);

        outboxService.record("Livro", livroAtualizado.getId().toString(), "LivroAtualizado",
                livroMapper.livroParaDocument(livroAtualizado), Broker.KAFKA, TOPICO);

        return livroMapper.livroParaResponse(livroAtualizado);

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

        outboxService.record("Livro", livroDeletar.getId().toString(), "LivroExcluido",
                livroMapper.livroParaDocument(livroDeletar), Broker.KAFKA, TOPICO);

        outboxService.record("Livro", livroDeletar.getId().toString(), "ArquivoParaExcluir",
                new DeletarArquivoCommand(BucketSupabase.LIVRO_CAPA, livroDeletar.getCapa()),
                Broker.RABBITMQ, FILA_DELETE);

    }

    private UploadArquivoCommand comandoUpload(MultipartFile arquivo, BucketSupabase bucket, String nome)
            throws IOException {

        return new UploadArquivoCommand(bucket, nome, arquivo.getContentType(),
                Base64.getEncoder().encodeToString(arquivo.getBytes()));

    }
}
