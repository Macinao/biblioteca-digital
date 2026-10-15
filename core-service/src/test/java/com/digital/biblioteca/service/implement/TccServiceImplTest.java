package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.mapper.TccMapper;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.model.Tcc;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.repository.TccRepository;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TccServiceImplTest {

    @Mock TccRepository tccRepository;
    @Mock TccMapper tccMapper;
    @Mock OutboxService outboxService;
    @Mock TemaRepository temaRepository;
    @Mock CursoRepository cursoRepository;
    @InjectMocks TccServiceImpl service;

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("conteudo", "tcc.pdf", "application/pdf", "pdf".getBytes());
    }

    private static Tcc existente() {
        Tcc tcc = new Tcc();
        tcc.setId(UUID.randomUUID());
        tcc.setConteudo("conteudo-existente");
        tcc.setAtivo(true);
        return tcc;
    }

    @Test
    void cadastrarVinculaCursoAtivoNomeiaOConteudoPeloIdEPublicaUploadEEventoDeDominio() throws Exception {
        CadastrarTccRequest request = new CadastrarTccRequest();
        request.setCurso(UUID.randomUUID());
        Tcc mapeado = new Tcc();
        Tcc salvo = new Tcc();
        salvo.setId(UUID.randomUUID());
        Curso curso = new Curso();
        when(tccMapper.cadastroRequestParaTcc(request)).thenReturn(mapeado);
        when(tccRepository.save(mapeado)).thenReturn(salvo);
        when(cursoRepository.findByIdAndAtivoTrue(request.getCurso())).thenReturn(Optional.of(curso));

        service.cadastrar(request, pdf());

        assertThat(salvo.getCurso()).isSameAs(curso);
        assertThat(salvo.getConteudo()).isEqualTo(salvo.getId().toString());
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType).containsExactly("ArquivoParaSubir", "TccCadastrado");
        assertThat(eventos.get(0).payload()).isEqualTo(new UploadArquivoCommand(
                BucketSupabase.TCC_CONTEUDO, salvo.getId().toString(), "application/pdf",
                java.util.Base64.getEncoder().encodeToString("pdf".getBytes())));
        assertThat(eventos.get(1).destination()).isEqualTo("tcc-events");
    }

    @Test
    void cadastrarComCursoInexistenteFalhaSemPublicarNada() {
        CadastrarTccRequest request = new CadastrarTccRequest();
        request.setCurso(UUID.randomUUID());
        Tcc salvo = new Tcc();
        salvo.setId(UUID.randomUUID());
        when(tccMapper.cadastroRequestParaTcc(request)).thenReturn(new Tcc());
        when(tccRepository.save(org.mockito.ArgumentMatchers.any(Tcc.class))).thenReturn(salvo);
        when(cursoRepository.findByIdAndAtivoTrue(request.getCurso())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cadastrar(request, pdf()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Curso não encontrado");

        verifyNoInteractions(outboxService);
    }

    @Test
    void atualizarFalhaQuandoOTccAtivoNaoExiste() {
        AtualizarTccRequest request = new AtualizarTccRequest();
        request.setId(UUID.randomUUID());
        when(tccRepository.findByIdAndAtivoTrue(request.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(request, pdf()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("TCC não encontrado");
    }

    @Test
    void atualizarComCursoInexistenteFalhaSemPublicarNada() {
        Tcc tcc = existente();
        AtualizarTccRequest request = new AtualizarTccRequest();
        request.setId(tcc.getId());
        request.setCurso(UUID.randomUUID());
        when(tccRepository.findByIdAndAtivoTrue(tcc.getId())).thenReturn(Optional.of(tcc));
        when(cursoRepository.findByIdAndAtivoTrue(request.getCurso())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(request, pdf()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Curso não encontrado");

        verifyNoInteractions(outboxService);
    }

    @Test
    void atualizarComNovoConteudoPublicaUploadComONomeExistenteEOEventoDeDominio() throws Exception {
        Tcc tcc = existente();
        AtualizarTccRequest request = new AtualizarTccRequest();
        request.setId(tcc.getId());
        request.setCurso(UUID.randomUUID());
        Curso curso = new Curso();
        when(tccRepository.findByIdAndAtivoTrue(tcc.getId())).thenReturn(Optional.of(tcc));
        when(cursoRepository.findByIdAndAtivoTrue(request.getCurso())).thenReturn(Optional.of(curso));

        service.atualizar(request, pdf());

        assertThat(tcc.getCurso()).isSameAs(curso);
        verify(tccMapper).atualizarRequestParaTcc(request, tcc);
        verify(tccRepository).save(tcc);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType).containsExactly("ArquivoParaSubir", "TccAtualizado");
        assertThat(((UploadArquivoCommand) eventos.get(0).payload()).nome()).isEqualTo("conteudo-existente");
    }

    @Test
    void atualizarSemConteudoNaoPublicaUpload() throws Exception {
        Tcc tcc = existente();
        AtualizarTccRequest request = new AtualizarTccRequest();
        request.setId(tcc.getId());
        request.setCurso(UUID.randomUUID());
        when(tccRepository.findByIdAndAtivoTrue(tcc.getId())).thenReturn(Optional.of(tcc));
        when(cursoRepository.findByIdAndAtivoTrue(request.getCurso())).thenReturn(Optional.of(new Curso()));

        service.atualizar(request, null);

        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("TccAtualizado");
    }

    @Test
    void deletarFalhaQuandoOTccAtivoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(tccRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deletarFazExclusaoLogicaEPedeAExclusaoDoConteudo() {
        Tcc tcc = existente();
        when(tccRepository.findByIdAndAtivoTrue(tcc.getId())).thenReturn(Optional.of(tcc));

        service.deletar(tcc.getId());

        assertThat(tcc.isAtivo()).isFalse();
        verify(tccRepository).save(tcc);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType).containsExactly("TccExcluido", "ArquivoParaExcluir");
        assertThat(eventos.get(1).payload()).isEqualTo(new DeletarArquivoCommand(BucketSupabase.TCC_CONTEUDO, "conteudo-existente"));
    }
}
