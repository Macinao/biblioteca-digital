package com.digital.biblioteca.service.implement;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.biblioteca.mapper.TccMapper;
import com.digital.biblioteca.model.Tcc;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.response.TccResponse;
import com.digital.biblioteca.service.TccService;
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
public class TccServiceImpl implements TccService {

    private static final String TOPICO = "tcc-events";
    private static final String FILA_UPLOAD = "storage.upload";
    private static final String FILA_DELETE = "storage.delete";

    private final TccRepository tccRepository;
    private final TccMapper tccMapper;
    private final OutboxService outboxService;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;

    @Override
    public TccResponse cadastrar(CadastrarTccRequest request, MultipartFile conteudo) throws IOException {

        Tcc novoTcc = tccRepository.save(
                tccMapper.cadastroRequestParaTcc(request));

        novoTcc.setTemas(temaRepository.findAllById(request.getTemas()));

        novoTcc.setCurso(cursoRepository.findByIdAndAtivoTrue(request.getCurso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado")));

        novoTcc.setConteudo(novoTcc.getId().toString());

        outboxService.record("Tcc", novoTcc.getId().toString(), "ArquivoParaSubir",
                comandoUpload(conteudo, BucketSupabase.TCC_CONTEUDO, novoTcc.getConteudo()),
                Broker.RABBITMQ, FILA_UPLOAD);

        outboxService.record("Tcc", novoTcc.getId().toString(), "TccCadastrado",
                tccMapper.tccParaDocument(novoTcc), Broker.KAFKA, TOPICO);

        return tccMapper.tccParaResponse(novoTcc);

    }

    @Override
    public TccResponse atualizar(AtualizarTccRequest request, MultipartFile conteudo) throws IOException {

        Tcc tcctualizado = tccRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("TCC não encontrado"));

        tcctualizado.setTemas(temaRepository.findAllById(request.getTemas()));

        tcctualizado.setCurso(cursoRepository.findByIdAndAtivoTrue(request.getCurso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado")));

        tccMapper.atualizarRequestParaTcc(request, tcctualizado);

        if (conteudo != null && !conteudo.isEmpty()) {
            outboxService.record("Tcc", tcctualizado.getId().toString(), "ArquivoParaSubir",
                    comandoUpload(conteudo, BucketSupabase.TCC_CONTEUDO, tcctualizado.getConteudo()),
                    Broker.RABBITMQ, FILA_UPLOAD);
        }

        tccRepository.save(tcctualizado);

        outboxService.record("Tcc", tcctualizado.getId().toString(), "TccAtualizado",
                tccMapper.tccParaDocument(tcctualizado), Broker.KAFKA, TOPICO);

        return tccMapper.tccParaResponse(tcctualizado);

    }

    @Override
    public void deletar(UUID id) {

        Tcc tccDeletar = tccRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("TCC não encontrado"));

        tccDeletar.setAtivo(false);

        tccRepository.save(tccDeletar);

        outboxService.record("Tcc", tccDeletar.getId().toString(), "TccExcluido",
                tccMapper.tccParaDocument(tccDeletar), Broker.KAFKA, TOPICO);

        outboxService.record("Tcc", tccDeletar.getId().toString(), "ArquivoParaExcluir",
                new DeletarArquivoCommand(BucketSupabase.TCC_CONTEUDO, tccDeletar.getConteudo()),
                Broker.RABBITMQ, FILA_DELETE);

    }

    private UploadArquivoCommand comandoUpload(MultipartFile arquivo, BucketSupabase bucket, String nome)
            throws IOException {

        return new UploadArquivoCommand(bucket, nome, arquivo.getContentType(),
                Base64.getEncoder().encodeToString(arquivo.getBytes()));

    }
}
