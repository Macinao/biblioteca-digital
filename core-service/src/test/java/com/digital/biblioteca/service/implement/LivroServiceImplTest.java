package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.document.LivroDocument;
import com.digital.biblioteca.mapper.LivroMapper;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.repository.LivroRepository;
import com.digital.biblioteca.repository.ReservaRepository;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.request.AtualizarLivroRequest;
import com.digital.biblioteca.request.CadastrarLivroRequest;
import com.digital.biblioteca.response.LivroResponse;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;
import java.util.List;
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
class LivroServiceImplTest {

    @Mock LivroRepository livroRepository;
    @Mock LivroMapper livroMapper;
    @Mock OutboxService outboxService;
    @Mock TemaRepository temaRepository;
    @Mock CursoRepository cursoRepository;
    @Mock ReservaRepository reservaRepository;
    @InjectMocks LivroServiceImpl service;

    private static Livro livro() {
        Livro livro = new Livro();
        livro.setId(UUID.randomUUID());
        livro.setCapa("capa-existente");
        livro.setAtivo(true);
        return livro;
    }

    private static MockMultipartFile capa() {
        return new MockMultipartFile("capa", "capa.png", "image/png", "bytes-da-capa".getBytes());
    }

    // ---------- cadastrar ----------

    @Test
    void cadastrarSalvaVinculaTemasECursosNomeiaACapaPeloIdEPublicaUploadSeguidoDoEventoDeDominio() throws Exception {
        CadastrarLivroRequest request = new CadastrarLivroRequest();
        request.setTemas(List.of(UUID.randomUUID()));
        request.setCursos(List.of(UUID.randomUUID()));
        Livro mapeado = new Livro();
        Livro salvo = new Livro();
        salvo.setId(UUID.randomUUID());
        List<Tema> temas = List.of(new Tema());
        List<Curso> cursos = List.of(new Curso());
        LivroDocument document = new LivroDocument();
        LivroResponse response = new LivroResponse();
        when(livroMapper.cadastroRequestParaLivro(request)).thenReturn(mapeado);
        when(livroRepository.save(mapeado)).thenReturn(salvo);
        when(temaRepository.findAllById(request.getTemas())).thenReturn(temas);
        when(cursoRepository.findAllById(request.getCursos())).thenReturn(cursos);
        when(livroMapper.livroParaDocument(salvo)).thenReturn(document);
        when(livroMapper.livroParaResponse(salvo)).thenReturn(response);

        LivroResponse resultado = service.cadastrar(request, capa());

        assertThat(resultado).isSameAs(response);
        assertThat(salvo.getTemas()).isSameAs(temas);
        assertThat(salvo.getCursos()).isSameAs(cursos);
        assertThat(salvo.getCapa()).isEqualTo(salvo.getId().toString());

        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).hasSize(2);

        var upload = eventos.get(0);
        assertThat(upload.eventType()).isEqualTo("ArquivoParaSubir");
        assertThat(upload.broker()).isEqualTo(Broker.RABBITMQ);
        assertThat(upload.destination()).isEqualTo("storage.upload");
        assertThat(upload.payload()).isEqualTo(new UploadArquivoCommand(
                BucketSupabase.LIVRO_CAPA, salvo.getId().toString(), "image/png",
                Base64.getEncoder().encodeToString("bytes-da-capa".getBytes())));

        var dominio = eventos.get(1);
        assertThat(dominio.eventType()).isEqualTo("LivroCadastrado");
        assertThat(dominio.broker()).isEqualTo(Broker.KAFKA);
        assertThat(dominio.destination()).isEqualTo("livro-events");
        assertThat(dominio.payload()).isSameAs(document);
        assertThat(dominio.aggregateId()).isEqualTo(salvo.getId().toString());
    }

    // ---------- atualizar ----------

    @Test
    void atualizarFalhaQuandoOLivroAtivoNaoExiste() {
        AtualizarLivroRequest request = new AtualizarLivroRequest();
        request.setId(UUID.randomUUID());
        when(livroRepository.findByIdAndAtivoTrue(request.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(request, capa()))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Livro não encontrado");

        verifyNoInteractions(outboxService);
    }

    @Test
    void atualizarComNovaCapaPublicaUploadComONomeDaCapaExistenteEDepoisOEventoDeDominio() throws Exception {
        Livro existente = livro();
        AtualizarLivroRequest request = new AtualizarLivroRequest();
        request.setId(existente.getId());
        when(livroRepository.findByIdAndAtivoTrue(existente.getId())).thenReturn(Optional.of(existente));

        service.atualizar(request, capa());

        verify(livroMapper).atualizarRequestParaLivro(request, existente);
        verify(livroRepository).save(existente);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType).containsExactly("ArquivoParaSubir", "LivroAtualizado");
        assertThat(((UploadArquivoCommand) eventos.get(0).payload()).nome()).isEqualTo("capa-existente");
    }

    @Test
    void atualizarSemCapaNaoPublicaUploadMasPublicaOEventoDeDominio() throws Exception {
        Livro existente = livro();
        AtualizarLivroRequest request = new AtualizarLivroRequest();
        request.setId(existente.getId());
        when(livroRepository.findByIdAndAtivoTrue(existente.getId())).thenReturn(Optional.of(existente));

        service.atualizar(request, null);

        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("LivroAtualizado");
    }

    @Test
    void atualizarComCapaVaziaTrataComoSemCapa() throws Exception {
        Livro existente = livro();
        AtualizarLivroRequest request = new AtualizarLivroRequest();
        request.setId(existente.getId());
        when(livroRepository.findByIdAndAtivoTrue(existente.getId())).thenReturn(Optional.of(existente));

        service.atualizar(request, new MockMultipartFile("capa", "capa.png", "image/png", new byte[0]));

        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("LivroAtualizado");
    }

    // ---------- deletar ----------

    @Test
    void deletarFalhaQuandoOLivroAtivoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(livroRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(id)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deletarBloqueiaQuandoHaReservasAtivasENaoAlteraOLivro() {
        Livro livro = livro();
        when(livroRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));
        when(reservaRepository.existsByLivroIdAndAtivoTrue(livro.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.deletar(livro.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não é possível deletar um livro que possui reservas ativas.");

        assertThat(livro.isAtivo()).isTrue();
        verify(livroRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void deletarFazExclusaoLogicaPublicaEventoDeDominioEPedidoDeExclusaoDaCapa() {
        Livro livro = livro();
        when(livroRepository.findByIdAndAtivoTrue(livro.getId())).thenReturn(Optional.of(livro));
        when(reservaRepository.existsByLivroIdAndAtivoTrue(livro.getId())).thenReturn(false);

        service.deletar(livro.getId());

        assertThat(livro.isAtivo()).isFalse();
        verify(livroRepository).save(livro);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType).containsExactly("LivroExcluido", "ArquivoParaExcluir");
        assertThat(eventos.get(0).broker()).isEqualTo(Broker.KAFKA);
        assertThat(eventos.get(0).destination()).isEqualTo("livro-events");
        assertThat(eventos.get(1).broker()).isEqualTo(Broker.RABBITMQ);
        assertThat(eventos.get(1).destination()).isEqualTo("storage.delete");
        assertThat(eventos.get(1).payload()).isEqualTo(new DeletarArquivoCommand(BucketSupabase.LIVRO_CAPA, "capa-existente"));
    }
}
