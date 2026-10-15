package com.digital.biblioteca.service.implement;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.biblioteca.mapper.LivroPublicoMapper;
import com.digital.biblioteca.model.LivroPublico;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.response.LivroPublicoResponse;
import com.digital.biblioteca.service.LivroPublicoService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
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
public class LivroPublicoServiceImpl implements LivroPublicoService {

    private static final String TOPICO = "livro-publico-events";
    private static final String FILA_UPLOAD = "storage.upload";
    private static final String FILA_DELETE = "storage.delete";

    private final LivroPublicoRepository livroPublicoRepository;
    private final LivroPublicoMapper livroPublicoMapper;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;
    private final OutboxService outboxService;

    @Override
    public LivroPublicoResponse cadastrar(CadastrarLivroPublicoRequest request,
                                          MultipartFile conteudo, MultipartFile capa) throws IOException {

        LivroPublico novoLivro = livroPublicoRepository.save(
                livroPublicoMapper.cadastroRequestParaLivroPublico(request));

        novoLivro.setTemas(temaRepository.findAllById(request.getTemas()));

        novoLivro.setCursos(cursoRepository.findAllById(request.getCursos()));

        novoLivro.setCapa(novoLivro.getId().toString());

        novoLivro.setConteudo(novoLivro.getId().toString());

        outboxService.record("LivroPublico", novoLivro.getId().toString(), "ArquivoParaSubir",
                comandoUpload(conteudo, BucketSupabase.LIVRO_PUBLICO_CONTEUDO, novoLivro.getConteudo()),
                Broker.RABBITMQ, FILA_UPLOAD);

        outboxService.record("LivroPublico", novoLivro.getId().toString(), "ArquivoParaSubir",
                comandoUpload(capa, BucketSupabase.LIVRO_PUBLICO_CAPA, novoLivro.getCapa()),
                Broker.RABBITMQ, FILA_UPLOAD);

        outboxService.record("LivroPublico", novoLivro.getId().toString(), "LivroPublicoCadastrado",
                livroPublicoMapper.livroPublicoParaDocument(novoLivro), Broker.KAFKA, TOPICO);

        return livroPublicoMapper.livroPublicoParaResponse(novoLivro);

    }

    @Override
    public LivroPublicoResponse atualizar(AtualizarLivroPublicoRequest request, MultipartFile conteudo, MultipartFile capa)
            throws IOException {

        LivroPublico livroAtualizado = livroPublicoRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro público não encontrado"));

        livroAtualizado.setTemas(temaRepository.findAllById(request.getTemas()));

        livroAtualizado.setCursos(cursoRepository.findAllById(request.getCursos()));

        livroPublicoMapper.atualizarRequestParaLivroPublico(request, livroAtualizado);

        if (conteudo != null && !conteudo.isEmpty()) {
            outboxService.record("LivroPublico", livroAtualizado.getId().toString(), "ArquivoParaSubir",
                    comandoUpload(conteudo, BucketSupabase.LIVRO_PUBLICO_CONTEUDO, livroAtualizado.getConteudo()),
                    Broker.RABBITMQ, FILA_UPLOAD);
        }

        if (capa != null && !capa.isEmpty()) {
            outboxService.record("LivroPublico", livroAtualizado.getId().toString(), "ArquivoParaSubir",
                    comandoUpload(capa, BucketSupabase.LIVRO_PUBLICO_CAPA, livroAtualizado.getCapa()),
                    Broker.RABBITMQ, FILA_UPLOAD);
        }

        livroPublicoRepository.save(livroAtualizado);

        outboxService.record("LivroPublico", livroAtualizado.getId().toString(), "LivroPublicoAtualizado",
                livroPublicoMapper.livroPublicoParaDocument(livroAtualizado), Broker.KAFKA, TOPICO);

        return livroPublicoMapper.livroPublicoParaResponse(livroAtualizado);

    }

    @Override
    public void deletar(UUID id) {

        LivroPublico livroDeletar = livroPublicoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro público não encontrado"));

        livroDeletar.setAtivo(false);

        livroPublicoRepository.save(livroDeletar);

        outboxService.record("LivroPublico", livroDeletar.getId().toString(), "LivroPublicoExcluido",
                livroPublicoMapper.livroPublicoParaDocument(livroDeletar), Broker.KAFKA, TOPICO);

        outboxService.record("LivroPublico", livroDeletar.getId().toString(), "ArquivoParaExcluir",
                new DeletarArquivoCommand(BucketSupabase.LIVRO_PUBLICO_CONTEUDO, livroDeletar.getConteudo()),
                Broker.RABBITMQ, FILA_DELETE);

        outboxService.record("LivroPublico", livroDeletar.getId().toString(), "ArquivoParaExcluir",
                new DeletarArquivoCommand(BucketSupabase.LIVRO_PUBLICO_CAPA, livroDeletar.getCapa()),
                Broker.RABBITMQ, FILA_DELETE);

    }

    private UploadArquivoCommand comandoUpload(MultipartFile arquivo, BucketSupabase bucket, String nome)
            throws IOException {

        return new UploadArquivoCommand(bucket, nome, arquivo.getContentType(),
                Base64.getEncoder().encodeToString(arquivo.getBytes()));

    }
}
