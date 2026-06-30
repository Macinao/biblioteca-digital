package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.eventlistener.TemaAtualizarEvent;
import com.digital.biblioteca.eventlistener.TemaCadastroEvent;
import com.digital.biblioteca.eventlistener.TemaExcluirEvent;
import com.digital.biblioteca.mapper.TemaMapper;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.repository.TemaDocumentRepository;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.request.AtualizarTemaRequest;
import com.digital.biblioteca.request.CadastrarTemaRequest;
import com.digital.biblioteca.response.TemaResponse;
import com.digital.biblioteca.service.TemaService;
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
public class TemaServiceImpl implements TemaService {

    private final TemaRepository temaRepository;
    private final TemaDocumentRepository temaDocumentRepository;
    private final TemaMapper temaMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public TemaResponse cadastrar(CadastrarTemaRequest request) {

        if (temaRepository.existsByNomeIgnoreCaseAndAtivoTrue(request.getNome())){
            throw new RegraNegocioException("Já existe um tema ativo com esse nome.");
        }

        Tema novoTema = temaRepository.save(
                    temaMapper.cadastroRequestParaTema(request));

        eventPublisher.publishEvent(new TemaCadastroEvent(novoTema));

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

        eventPublisher.publishEvent(new TemaAtualizarEvent(
                temaMapper.temaParaDocument(temaAtualizado)));

        return temaMapper.temaParaResponse(temaAtualizado);
    }

    @Override
    public List<TemaResponse> listar() {

        return temaMapper.listaDocumentParaResponse(
                temaDocumentRepository.findAll());

    }

    @Override
    public void deletar(UUID id) {

        Tema excluir = temaRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Tema não encontrado"));

        excluir.setAtivo(false);

        temaRepository.save(excluir);

        eventPublisher.publishEvent(new TemaExcluirEvent(
                temaMapper.temaParaDocument(excluir)));

    }
}
