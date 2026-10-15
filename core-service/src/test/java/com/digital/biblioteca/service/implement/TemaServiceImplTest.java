package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.document.TemaDocument;
import com.digital.biblioteca.mapper.TemaMapper;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.request.AtualizarTemaRequest;
import com.digital.biblioteca.request.CadastrarTemaRequest;
import com.digital.biblioteca.response.TemaResponse;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemaServiceImplTest {

    @Mock TemaRepository temaRepository;
    @Mock TemaMapper temaMapper;
    @Mock OutboxService outboxService;
    @InjectMocks TemaServiceImpl service;

    private Tema tema(String nome) {
        Tema tema = new Tema();
        tema.setId(UUID.randomUUID());
        tema.setNome(nome);
        tema.setAtivo(true);
        return tema;
    }

    @Test
    void cadastrarRejeitaNomeJaUsadoPorTemaAtivo() {
        CadastrarTemaRequest request = new CadastrarTemaRequest();
        request.setNome("Algoritmos");
        when(temaRepository.existsByNomeIgnoreCaseAndAtivoTrue("Algoritmos")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um tema ativo com esse nome.");

        verify(temaRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void cadastrarSalvaEPublicaTemaCadastradoNoKafka() {
        CadastrarTemaRequest request = new CadastrarTemaRequest();
        request.setNome("Algoritmos");
        Tema mapeado = new Tema();
        Tema salvo = tema("Algoritmos");
        TemaDocument document = new TemaDocument();
        TemaResponse response = new TemaResponse();
        when(temaRepository.existsByNomeIgnoreCaseAndAtivoTrue("Algoritmos")).thenReturn(false);
        when(temaMapper.cadastroRequestParaTema(request)).thenReturn(mapeado);
        when(temaRepository.save(mapeado)).thenReturn(salvo);
        when(temaMapper.temaParaDocument(salvo)).thenReturn(document);
        when(temaMapper.temaParaResponse(salvo)).thenReturn(response);

        TemaResponse resultado = service.cadastrar(request);

        assertThat(resultado).isSameAs(response);
        assertThat(OutboxCalls.de(outboxService)).singleElement().satisfies(e -> {
            assertThat(e.aggregateType()).isEqualTo("Tema");
            assertThat(e.aggregateId()).isEqualTo(salvo.getId().toString());
            assertThat(e.eventType()).isEqualTo("TemaCadastrado");
            assertThat(e.payload()).isSameAs(document);
            assertThat(e.broker()).isEqualTo(Broker.KAFKA);
            assertThat(e.destination()).isEqualTo("tema-events");
        });
    }

    @Test
    void atualizarFalhaQuandoOTemaAtivoNaoExiste() {
        AtualizarTemaRequest request = new AtualizarTemaRequest();
        request.setTemaId(UUID.randomUUID());
        request.setNovoTema("Novo");
        when(temaRepository.findByIdAndAtivoTrue(request.getTemaId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Tema não encontrado");
    }

    @Test
    void atualizarRejeitaNomeDeOutroTemaAtivo() {
        Tema existente = tema("Algoritmos");
        AtualizarTemaRequest request = new AtualizarTemaRequest();
        request.setTemaId(existente.getId());
        request.setNovoTema("Redes");
        when(temaRepository.findByIdAndAtivoTrue(existente.getId())).thenReturn(Optional.of(existente));
        when(temaRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot("Redes", existente.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(request)).isInstanceOf(RegraNegocioException.class);

        assertThat(existente.getNome()).isEqualTo("Algoritmos");
        verifyNoInteractions(outboxService);
    }

    @Test
    void atualizarTrocaONomeSalvaEPublicaTemaAtualizado() {
        Tema existente = tema("Algoritmos");
        AtualizarTemaRequest request = new AtualizarTemaRequest();
        request.setTemaId(existente.getId());
        request.setNovoTema("Estruturas de Dados");
        when(temaRepository.findByIdAndAtivoTrue(existente.getId())).thenReturn(Optional.of(existente));
        when(temaRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot("Estruturas de Dados", existente.getId())).thenReturn(false);

        service.atualizar(request);

        assertThat(existente.getNome()).isEqualTo("Estruturas de Dados");
        verify(temaRepository).save(existente);
        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("TemaAtualizado");
    }

    @Test
    void deletarFalhaQuandoOTemaAtivoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(temaRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(id)).isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(outboxService);
    }

    @Test
    void deletarFazExclusaoLogicaEPublicaTemaExcluido() {
        Tema tema = tema("Algoritmos");
        when(temaRepository.findByIdAndAtivoTrue(tema.getId())).thenReturn(Optional.of(tema));

        service.deletar(tema.getId());

        assertThat(tema.isAtivo()).isFalse();
        verify(temaRepository).save(tema);
        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("TemaExcluido");
    }
}
