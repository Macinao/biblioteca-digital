package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.eventlistener.*;
import com.digital.biblioteca.mapper.CursoMapper;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.repository.CursoDocumentRepository;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.request.AtualizarCursoRequest;
import com.digital.biblioteca.request.CadastrarCursoRequest;
import com.digital.biblioteca.response.CursoResponse;
import com.digital.biblioteca.service.CursoService;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursoRepository;
    private final CursoMapper cursoMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final CursoDocumentRepository cursoDocumentRepository;

    @Override
    public CursoResponse cadastrar(CadastrarCursoRequest request) {

        if (cursoRepository.existsByNomeIgnoreCaseAndAtivoTrue(request.getNome())){
            throw new RegraNegocioException("Já existe um curso ativo com esse nome.");
        }

        Curso novoCurso = cursoRepository.save(cursoMapper
                .cadastroRequestParaCurso(request));

        eventPublisher.publishEvent(new CursoCadastroEvent(novoCurso));

        return cursoMapper.cursoParaResponse(novoCurso);

    }

    @Override
    public CursoResponse atualizar(AtualizarCursoRequest request) {

        Curso cursoAtualizado = cursoRepository
                .findByIdAndAtivoTrue(request.getCursoId())
                .orElseThrow(() -> new EntityNotFoundException("Curso não encontrado"));

        if (cursoRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot(
                request.getNovoCurso(), request.getCursoId())) {
            throw new RegraNegocioException("Já existe um curso ativo com esse nome.");
        }

        cursoAtualizado.setNome(request.getNovoCurso());

        cursoRepository.save(cursoAtualizado);

        eventPublisher.publishEvent(new CursoAtualizarEvent(
                cursoMapper.cursoParaDocument(cursoAtualizado)));

        return cursoMapper.cursoParaResponse(cursoAtualizado);

    }

    @Override
    public List<CursoResponse> listar() {

        return cursoMapper.listaDocumentParaResponse(
                cursoDocumentRepository.findAll());

    }

    @Override
    public void deletar(UUID id) {

        Curso excluir = cursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Curso não encontrado"));

        excluir.setAtivo(false);

        cursoRepository.save(excluir);

        eventPublisher.publishEvent(new CursoExcluirEvent(
                cursoMapper.cursoParaDocument(excluir)));

    }
}
