package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.mapper.LivroPublicoMapper;
import com.digital.biblioteca.model.LivroPublico;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.repository.LivroPublicoRepository;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
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
class LivroPublicoServiceImplTest {

    @Mock LivroPublicoRepository livroPublicoRepository;
    @Mock LivroPublicoMapper livroPublicoMapper;
    @Mock TemaRepository temaRepository;
    @Mock CursoRepository cursoRepository;
    @Mock OutboxService outboxService;
    @InjectMocks LivroPublicoServiceImpl service;

    private static LivroPublico existente() {
        LivroPublico livro = new LivroPublico();
        livro.setId(UUID.randomUUID());
        livro.setCapa("capa-existente");
        livro.setConteudo("conteudo-existente");
        livro.setAtivo(true);
        return livro;
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("conteudo", "livro.pdf", "application/pdf", "pdf".getBytes());
    }

    private static MockMultipartFile capa() {
        return new MockMultipartFile("capa", "capa.png", "image/png", "png".getBytes());
    }

    @Test
    void cadastrarNomeiaCapaEConteudoPeloIdEPublicaDoisUploadsEOEventoDeDominio() throws Exception {
        CadastrarLivroPublicoRequest request = new CadastrarLivroPublicoRequest();
        LivroPublico mapeado = new LivroPublico();
        LivroPublico salvo = new LivroPublico();
        salvo.setId(UUID.randomUUID());
        when(livroPublicoMapper.cadastroRequestParaLivroPublico(request)).thenReturn(mapeado);
        when(livroPublicoRepository.save(mapeado)).thenReturn(salvo);

        service.cadastrar(request, pdf(), capa());

        assertThat(salvo.getCapa()).isEqualTo(salvo.getId().toString());
        assertThat(salvo.getConteudo()).isEqualTo(salvo.getId().toString());
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType)
                .containsExactly("ArquivoParaSubir", "ArquivoParaSubir", "LivroPublicoCadastrado");
        assertThat(((UploadArquivoCommand) eventos.get(0).payload()).bucket()).isEqualTo(BucketSupabase.LIVRO_PUBLICO_CONTEUDO);
        assertThat(((UploadArquivoCommand) eventos.get(0).payload()).contentType()).isEqualTo("application/pdf");
        assertThat(((UploadArquivoCommand) eventos.get(1).payload()).bucket()).isEqualTo(BucketSupabase.LIVRO_PUBLICO_CAPA);
        assertThat(eventos.get(2).destination()).isEqualTo("livro-publico-events");
    }

    @Test
    void atualizarFalhaQuandoOLivroPublicoAtivoNaoExiste() {
        AtualizarLivroPublicoRequest request = new AtualizarLivroPublicoRequest();
        request.setId(UUID.randomUUID());
        when(livroPublicoRepository.findByIdAndAtivoTrue(request.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(request, pdf(), capa()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Livro público não encontrado");

        verifyNoInteractions(outboxService);
    }

    @Test
    void atualizarComConteudoECapaPublicaDoisUploadsReaproveitandoOsNomesExistentes() throws Exception {
        LivroPublico livro = existente();
        AtualizarLivroPublicoRequest request = new AtualizarLivroPublicoRequest();
        request.setId(livro.getId());
        when(livroPublicoRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));

        service.atualizar(request, pdf(), capa());

        verify(livroPublicoMapper).atualizarRequestParaLivroPublico(request, livro);
        verify(livroPublicoRepository).save(livro);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType)
                .containsExactly("ArquivoParaSubir", "ArquivoParaSubir", "LivroPublicoAtualizado");
        assertThat(((UploadArquivoCommand) eventos.get(0).payload()).nome()).isEqualTo("conteudo-existente");
        assertThat(((UploadArquivoCommand) eventos.get(1).payload()).nome()).isEqualTo("capa-existente");
    }

    @Test
    void atualizarSoComConteudoPublicaApenasUmUpload() throws Exception {
        LivroPublico livro = existente();
        AtualizarLivroPublicoRequest request = new AtualizarLivroPublicoRequest();
        request.setId(livro.getId());
        when(livroPublicoRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));

        service.atualizar(request, pdf(), null);

        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).hasSize(2);
        assertThat(((UploadArquivoCommand) eventos.get(0).payload()).bucket()).isEqualTo(BucketSupabase.LIVRO_PUBLICO_CONTEUDO);
    }

    @Test
    void atualizarSoComCapaPublicaApenasUmUpload() throws Exception {
        LivroPublico livro = existente();
        AtualizarLivroPublicoRequest request = new AtualizarLivroPublicoRequest();
        request.setId(livro.getId());
        when(livroPublicoRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));

        service.atualizar(request, null, capa());

        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).hasSize(2);
        assertThat(((UploadArquivoCommand) eventos.get(0).payload()).bucket()).isEqualTo(BucketSupabase.LIVRO_PUBLICO_CAPA);
    }

    @Test
    void atualizarSemArquivosPublicaSoOEventoDeDominio() throws Exception {
        LivroPublico livro = existente();
        AtualizarLivroPublicoRequest request = new AtualizarLivroPublicoRequest();
        request.setId(livro.getId());
        when(livroPublicoRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));

        service.atualizar(request, null, null);

        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("LivroPublicoAtualizado");
    }

    @Test
    void deletarFalhaQuandoOLivroPublicoAtivoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(livroPublicoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deletarFazExclusaoLogicaEPedeAExclusaoDoConteudoEDaCapa() {
        LivroPublico livro = existente();
        when(livroPublicoRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));

        service.deletar(livro.getId());

        assertThat(livro.isAtivo()).isFalse();
        verify(livroPublicoRepository).save(livro);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType)
                .containsExactly("LivroPublicoExcluido", "ArquivoParaExcluir", "ArquivoParaExcluir");
        assertThat(eventos.get(1).payload()).isEqualTo(new DeletarArquivoCommand(BucketSupabase.LIVRO_PUBLICO_CONTEUDO, "conteudo-existente"));
        assertThat(eventos.get(2).payload()).isEqualTo(new DeletarArquivoCommand(BucketSupabase.LIVRO_PUBLICO_CAPA, "capa-existente"));
    }
}
