package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.eventlistener.*;
import com.digital.biblioteca.mapper.TccMapper;
import com.digital.biblioteca.model.Tcc;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.response.TccResponse;
import com.digital.biblioteca.service.TccService;
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
public class TccServiceImpl implements TccService {

    private final TccRepository tccRepository;
    private final TccMapper tccMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;
    private final TccDocumentRepository tccDocumentRepository;

    @Override
    public TccResponse cadastrar(CadastrarTccRequest request, MultipartFile conteudo) {

        Tcc novoTcc = tccRepository.save(
                tccMapper.cadastroRequestParaTcc(request));

        novoTcc.setTemas(temaRepository.findAllById(request.getTemas()));

        novoTcc.setCurso(cursoRepository.findByIdAndAtivoTrue(request.getCurso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado")));

        novoTcc.setConteudo(novoTcc.getId().toString());

        eventPublisher.publishEvent(new SubirEvent(conteudo,
                BucketSupabase.TCC_CONTEUDO, novoTcc.getConteudo()));

        eventPublisher.publishEvent(new TccCadastrarEvent(
                tccMapper.tccParaDocument(novoTcc)));

        return tccMapper.tccParaResponse(novoTcc);

    }

    @Override
    public TccResponse atualizar(AtualizarTccRequest request, MultipartFile conteudo) {

        Tcc tcctualizado = tccRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("TCC não encontrado"));

        tcctualizado.setTemas(temaRepository.findAllById(request.getTemas()));

        tcctualizado.setCurso(cursoRepository.findByIdAndAtivoTrue(request.getCurso())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso não encontrado")));

        tccMapper.atualizarRequestParaTcc(request, tcctualizado);

        if (conteudo != null && !conteudo.isEmpty()) {
            eventPublisher.publishEvent(new SubirEvent(conteudo,
                    BucketSupabase.TCC_CONTEUDO, tcctualizado.getConteudo()));
        }

        tccRepository.save(tcctualizado);

        eventPublisher.publishEvent(new TccAtualizarEvent(
                tccMapper.tccParaDocument(tcctualizado)));

        return tccMapper.tccParaResponse(tcctualizado);

    }

    @Override
    public List<TccResponse> listar() {

        return tccMapper.listaDocumentParaResponse(
                tccDocumentRepository.findAll());

    }

    @Override
    public void deletar(UUID id) {

        Tcc tccDeletar = tccRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("TCC não encontrado"));

        tccDeletar.setAtivo(false);

        tccRepository.save(tccDeletar);

        eventPublisher.publishEvent(new TccDeletarEvent(
                tccMapper.tccParaDocument(tccDeletar)));

        eventPublisher.publishEvent(new DeletarArquivoEvent(
                tccDeletar.getConteudo(), BucketSupabase.TCC_CONTEUDO));

    }
}
