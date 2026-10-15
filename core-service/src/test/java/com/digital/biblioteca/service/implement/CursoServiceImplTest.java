package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.document.CursoDocument;
import com.digital.biblioteca.mapper.CursoMapper;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.request.AtualizarCursoRequest;
import com.digital.biblioteca.request.CadastrarCursoRequest;
import com.digital.biblioteca.response.CursoResponse;
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
class CursoServiceImplTest {

    @Mock CursoRepository cursoRepository;
    @Mock CursoMapper cursoMapper;
    @Mock OutboxService outboxService;
    @InjectMocks CursoServiceImpl service;

    private Curso curso(String nome) {
        Curso curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setNome(nome);
        curso.setAtivo(true);
        return curso;
    }

    // ---------- cadastrar ----------

    @Test
    void cadastrarRejeitaNomeJaUsadoPorCursoAtivoSemSalvarNemPublicar() {
        CadastrarCursoRequest request = new CadastrarCursoRequest();
        request.setNome("Direito");
        when(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrue("Direito")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um curso ativo com esse nome.");

        verify(cursoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void cadastrarSalvaPublicaCursoCadastradoNoKafkaEDevolveOResponse() {
        CadastrarCursoRequest request = new CadastrarCursoRequest();
        request.setNome("Direito");
        Curso mapeado = new Curso();
        Curso salvo = curso("Direito");
        CursoDocument document = new CursoDocument();
        CursoResponse response = new CursoResponse();
        when(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrue("Direito")).thenReturn(false);
        when(cursoMapper.cadastroRequestParaCurso(request)).thenReturn(mapeado);
        when(cursoRepository.save(mapeado)).thenReturn(salvo);
        when(cursoMapper.cursoParaDocument(salvo)).thenReturn(document);
        when(cursoMapper.cursoParaResponse(salvo)).thenReturn(response);

        CursoResponse resultado = service.cadastrar(request);

        assertThat(resultado).isSameAs(response);
        assertThat(OutboxCalls.de(outboxService)).singleElement().satisfies(e -> {
            assertThat(e.aggregateType()).isEqualTo("Curso");
            assertThat(e.aggregateId()).isEqualTo(salvo.getId().toString());
            assertThat(e.eventType()).isEqualTo("CursoCadastrado");
            assertThat(e.payload()).isSameAs(document);
            assertThat(e.broker()).isEqualTo(Broker.KAFKA);
            assertThat(e.destination()).isEqualTo("curso-events");
        });
    }

    // ---------- atualizar ----------

    @Test
    void atualizarFalhaQuandoOCursoAtivoNaoExiste() {
        AtualizarCursoRequest request = new AtualizarCursoRequest();
        request.setCursoId(UUID.randomUUID());
        request.setNovoCurso("Novo");
        when(cursoRepository.findByIdAndAtivoTrue(request.getCursoId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Curso não encontrado");

        verifyNoInteractions(outboxService);
    }

    @Test
    void atualizarRejeitaNomeDeOutroCursoAtivo() {
        Curso existente = curso("Direito");
        AtualizarCursoRequest request = new AtualizarCursoRequest();
        request.setCursoId(existente.getId());
        request.setNovoCurso("Medicina");
        when(cursoRepository.findByIdAndAtivoTrue(existente.getId())).thenReturn(Optional.of(existente));
        when(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot("Medicina", existente.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(request)).isInstanceOf(RegraNegocioException.class);

        assertThat(existente.getNome()).isEqualTo("Direito");
        verify(cursoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void atualizarTrocaONomeSalvaEPublicaCursoAtualizado() {
        Curso existente = curso("Direito");
        AtualizarCursoRequest request = new AtualizarCursoRequest();
        request.setCursoId(existente.getId());
        request.setNovoCurso("Direito Penal");
        when(cursoRepository.findByIdAndAtivoTrue(existente.getId())).thenReturn(Optional.of(existente));
        when(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot("Direito Penal", existente.getId())).thenReturn(false);
        CursoResponse response = new CursoResponse();
        when(cursoMapper.cursoParaResponse(existente)).thenReturn(response);

        CursoResponse resultado = service.atualizar(request);

        assertThat(resultado).isSameAs(response);
        assertThat(existente.getNome()).isEqualTo("Direito Penal");
        verify(cursoRepository).save(existente);
        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("CursoAtualizado");
    }

    // ---------- deletar ----------

    @Test
    void deletarFalhaQuandoOCursoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(cursoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(id)).isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(outboxService);
    }

    @Test
    void deletarFazExclusaoLogicaEPublicaCursoExcluido() {
        Curso curso = curso("Direito");
        when(cursoRepository.findById(curso.getId())).thenReturn(Optional.of(curso));

        service.deletar(curso.getId());

        assertThat(curso.isAtivo()).isFalse();
        verify(cursoRepository).save(curso);
        assertThat(OutboxCalls.de(outboxService)).singleElement().satisfies(e -> {
            assertThat(e.eventType()).isEqualTo("CursoExcluido");
            assertThat(e.broker()).isEqualTo(Broker.KAFKA);
            assertThat(e.destination()).isEqualTo("curso-events");
        });
    }

    @Test
    void deletarCursoJaInativoReemiteOEventoPoisABuscaNaoFiltraPorAtivo() {
        // Comportamento atual: ao contrario de TemaServiceImpl (findByIdAndAtivoTrue), o curso usa findById.
        Curso inativo = curso("Direito");
        inativo.setAtivo(false);
        when(cursoRepository.findById(inativo.getId())).thenReturn(Optional.of(inativo));

        service.deletar(inativo.getId());

        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("CursoExcluido");
    }
}
