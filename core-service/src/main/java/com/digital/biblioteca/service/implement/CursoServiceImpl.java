package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.mapper.CursoMapper;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.request.AtualizarCursoRequest;
import com.digital.biblioteca.request.CadastrarCursoRequest;
import com.digital.biblioteca.response.CursoResponse;
import com.digital.biblioteca.service.CursoService;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CursoServiceImpl implements CursoService {

    private static final String TOPICO = "curso-events";

    private final CursoRepository cursoRepository;
    private final CursoMapper cursoMapper;
    private final OutboxService outboxService;

    @Override
    public CursoResponse cadastrar(CadastrarCursoRequest request) {

        if (cursoRepository.existsByNomeIgnoreCaseAndAtivoTrue(request.getNome())){
            throw new RegraNegocioException("Já existe um curso ativo com esse nome.");
        }

        Curso novoCurso = cursoRepository.save(cursoMapper
                .cadastroRequestParaCurso(request));

        outboxService.record("Curso", novoCurso.getId().toString(), "CursoCadastrado",
                cursoMapper.cursoParaDocument(novoCurso), Broker.KAFKA, TOPICO);

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

        outboxService.record("Curso", cursoAtualizado.getId().toString(), "CursoAtualizado",
                cursoMapper.cursoParaDocument(cursoAtualizado), Broker.KAFKA, TOPICO);

        return cursoMapper.cursoParaResponse(cursoAtualizado);

    }

    @Override
    public void deletar(UUID id) {

        Curso excluir = cursoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Curso não encontrado"));

        excluir.setAtivo(false);

        cursoRepository.save(excluir);

        outboxService.record("Curso", excluir.getId().toString(), "CursoExcluido",
                cursoMapper.cursoParaDocument(excluir), Broker.KAFKA, TOPICO);

    }
}
