package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.mapper.TemaMapper;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.request.AtualizarTemaRequest;
import com.digital.biblioteca.request.CadastrarTemaRequest;
import com.digital.biblioteca.response.TemaResponse;
import com.digital.biblioteca.service.TemaService;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class TemaServiceImpl implements TemaService {

    private static final String TOPICO = "tema-events";

    private final TemaRepository temaRepository;
    private final TemaMapper temaMapper;
    private final OutboxService outboxService;

    @Override
    public TemaResponse cadastrar(CadastrarTemaRequest request) {

        if (temaRepository.existsByNomeIgnoreCaseAndAtivoTrue(request.getNome())){
            throw new RegraNegocioException("Já existe um tema ativo com esse nome.");
        }

        Tema novoTema = temaRepository.save(
                    temaMapper.cadastroRequestParaTema(request));

        outboxService.record("Tema", novoTema.getId().toString(), "TemaCadastrado",
                temaMapper.temaParaDocument(novoTema), Broker.KAFKA, TOPICO);

        return temaMapper.temaParaResponse(novoTema);
    }

    @Override
    public TemaResponse atualizar(AtualizarTemaRequest request) {

        Tema temaAtualizado = temaRepository
                .findByIdAndAtivoTrue(request.getTemaId())
                .orElseThrow(() -> new EntityNotFoundException("Tema não encontrado"));

        if (temaRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot(
                request.getNovoTema(), request.getTemaId())) {
            throw new RegraNegocioException("Já existe um tema ativo com esse nome.");
        }

        temaAtualizado.setNome(request.getNovoTema());

        temaRepository.save(temaAtualizado);

        outboxService.record("Tema", temaAtualizado.getId().toString(), "TemaAtualizado",
                temaMapper.temaParaDocument(temaAtualizado), Broker.KAFKA, TOPICO);

        return temaMapper.temaParaResponse(temaAtualizado);
    }

    @Override
    public void deletar(UUID id) {

        Tema excluir = temaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Tema não encontrado"));

        excluir.setAtivo(false);

        temaRepository.save(excluir);

        outboxService.record("Tema", excluir.getId().toString(), "TemaExcluido",
                temaMapper.temaParaDocument(excluir), Broker.KAFKA, TOPICO);

    }
}
