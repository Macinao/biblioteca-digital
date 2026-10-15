package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.mapper.MaterialEstudoMapper;
import com.digital.biblioteca.model.MaterialEstudo;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.repository.MaterialEstudoRepository;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

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
class MaterialEstudoServiceImplTest {

    @Mock MaterialEstudoRepository materialEstudoRepository;
    @Mock MaterialEstudoMapper materialEstudoMapper;
    @Mock OutboxService outboxService;
    @Mock TemaRepository temaRepository;
    @Mock CursoRepository cursoRepository;
    @InjectMocks MaterialEstudoServiceImpl service;

    private static Usuario usuario(Role role) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setRole(role);
        return usuario;
    }

    private static MaterialEstudo materialDe(Usuario criador) {
        MaterialEstudo material = new MaterialEstudo();
        material.setId(UUID.randomUUID());
        material.setConteudo("conteudo-existente");
        material.setCriador(criador);
        material.setAtivo(true);
        return material;
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("conteudo", "material.pdf", "application/pdf", "pdf".getBytes());
    }

    private static AtualizarMaterialEstudoRequest pedido(UUID id) {
        AtualizarMaterialEstudoRequest request = new AtualizarMaterialEstudoRequest();
        request.setId(id);
        return request;
    }

    // ---------- cadastrar ----------

    @Test
    void cadastrarRegistraOUsuarioLogadoComoCriadorENomeiaOConteudoPeloId() throws Exception {
        Usuario professor = usuario(Role.PROFESSOR);
        CadastrarMaterialEstudoRequest request = new CadastrarMaterialEstudoRequest();
        MaterialEstudo mapeado = new MaterialEstudo();
        MaterialEstudo salvo = new MaterialEstudo();
        salvo.setId(UUID.randomUUID());
        when(materialEstudoMapper.cadastroRequestParaMaterialEstudo(request)).thenReturn(mapeado);
        when(materialEstudoRepository.save(mapeado)).thenReturn(salvo);

        service.cadastrar(request, pdf(), professor);

        assertThat(salvo.getCriador()).isSameAs(professor);
        assertThat(salvo.getConteudo()).isEqualTo(salvo.getId().toString());
        verify(materialEstudoRepository).save(salvo);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType)
                .containsExactly("ArquivoParaSubir", "MaterialEstudoCadastrado");
        assertThat(eventos.get(1).destination()).isEqualTo("material-estudo-events");
    }

    // ---------- atualizar: permissao ----------

    @Test
    void atualizarFalhaQuandoOMaterialAtivoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(materialEstudoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(pedido(id), pdf(), usuario(Role.PROFESSOR)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Material de estudo não encontrado");
    }

    @Test
    void criadorPodeAtualizarOProprioMaterial() throws Exception {
        Usuario professor = usuario(Role.PROFESSOR);
        MaterialEstudo material = materialDe(professor);
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        service.atualizar(pedido(material.getId()), pdf(), professor);

        verify(materialEstudoMapper).atualizarRequestParaMaterialEstudo(any(), org.mockito.ArgumentMatchers.eq(material));
        verify(materialEstudoRepository).save(material);
        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("ArquivoParaSubir", "MaterialEstudoAtualizado");
    }

    @Test
    void bibliotecarioPodeAtualizarMaterialDeQualquerCriador() throws Exception {
        MaterialEstudo material = materialDe(usuario(Role.PROFESSOR));
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        service.atualizar(pedido(material.getId()), null, usuario(Role.BIBLIOTECARIO));

        verify(materialEstudoRepository).save(material);
        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("MaterialEstudoAtualizado");
    }

    @Test
    void outroProfessorNaoPodeAtualizarMaterialAlheio() {
        MaterialEstudo material = materialDe(usuario(Role.PROFESSOR));
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> service.atualizar(pedido(material.getId()), pdf(), usuario(Role.PROFESSOR)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Você só pode alterar materiais de estudo que você mesmo cadastrou.");

        verify(materialEstudoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void materialSemCriadorSoPodeSerAlteradoPeloBibliotecario() {
        MaterialEstudo material = materialDe(null);
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> service.atualizar(pedido(material.getId()), pdf(), usuario(Role.PROFESSOR)))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("não possui criador identificado");

        verifyNoInteractions(outboxService);
    }

    @Test
    void bibliotecarioPodeAlterarMaterialSemCriador() throws Exception {
        MaterialEstudo material = materialDe(null);
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        service.atualizar(pedido(material.getId()), null, usuario(Role.BIBLIOTECARIO));

        verify(materialEstudoRepository).save(material);
    }

    @Test
    void atualizarComArquivoVazioNaoPublicaUpload() throws Exception {
        Usuario professor = usuario(Role.PROFESSOR);
        MaterialEstudo material = materialDe(professor);
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        service.atualizar(pedido(material.getId()), new MockMultipartFile("c", "m.pdf", "application/pdf", new byte[0]), professor);

        assertThat(OutboxCalls.tipos(outboxService)).containsExactly("MaterialEstudoAtualizado");
    }

    // ---------- deletar ----------

    @Test
    void deletarFalhaQuandoOMaterialAtivoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(materialEstudoRepository.findByIdAndAtivoTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletar(id, usuario(Role.BIBLIOTECARIO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deletarMaterialAlheioComoProfessorEhNegadoENadaMuda() {
        MaterialEstudo material = materialDe(usuario(Role.PROFESSOR));
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        assertThatThrownBy(() -> service.deletar(material.getId(), usuario(Role.PROFESSOR)))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(material.isAtivo()).isTrue();
        verify(materialEstudoRepository, never()).save(any());
        verifyNoInteractions(outboxService);
    }

    @Test
    void criadorDeletaOProprioMaterialComExclusaoLogicaEPedidoDeExclusaoDoArquivo() {
        Usuario professor = usuario(Role.PROFESSOR);
        MaterialEstudo material = materialDe(professor);
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        service.deletar(material.getId(), professor);

        assertThat(material.isAtivo()).isFalse();
        verify(materialEstudoRepository).save(material);
        var eventos = OutboxCalls.de(outboxService);
        assertThat(eventos).extracting(OutboxCalls.Evento::eventType)
                .containsExactly("MaterialEstudoExcluido", "ArquivoParaExcluir");
        assertThat(eventos.get(1).payload())
                .isEqualTo(new DeletarArquivoCommand(BucketSupabase.MATERIAL_CONTEUDO, "conteudo-existente"));
    }

    @Test
    void bibliotecarioDeletaMaterialDeQualquerCriador() {
        MaterialEstudo material = materialDe(usuario(Role.PROFESSOR));
        when(materialEstudoRepository.findByIdAndAtivoTrue(material.getId())).thenReturn(Optional.of(material));

        service.deletar(material.getId(), usuario(Role.BIBLIOTECARIO));

        assertThat(material.isAtivo()).isFalse();
    }
}
