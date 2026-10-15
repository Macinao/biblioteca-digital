package com.digital.biblioteca.service.implement;

import com.digital.smartteca.common.commands.DeletarArquivoCommand;
import com.digital.smartteca.common.commands.UploadArquivoCommand;
import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.mapper.MaterialEstudoMapper;
import com.digital.biblioteca.model.MaterialEstudo;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.outbox.Broker;
import com.digital.biblioteca.outbox.OutboxService;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.response.MaterialEstudoResponse;
import com.digital.biblioteca.service.MaterialEstudoService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MaterialEstudoServiceImpl implements MaterialEstudoService {

    private static final String TOPICO = "material-estudo-events";
    private static final String FILA_UPLOAD = "storage.upload";
    private static final String FILA_DELETE = "storage.delete";

    private final MaterialEstudoRepository materialEstudoRepository;
    private final MaterialEstudoMapper materialEstudoMapper;
    private final OutboxService outboxService;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;

    @Override
    public MaterialEstudoResponse cadastrar(CadastrarMaterialEstudoRequest request,
                                            MultipartFile conteudo,
                                            Usuario usuario) throws IOException {

        MaterialEstudo novoMaterialEstudo = materialEstudoRepository.save(
                materialEstudoMapper.cadastroRequestParaMaterialEstudo(request));

        novoMaterialEstudo.setTemas(temaRepository.findAllById(request.getTemas()));

        novoMaterialEstudo.setCursos(cursoRepository.findAllById(request.getCursos()));

        novoMaterialEstudo.setConteudo(novoMaterialEstudo.getId().toString());

        novoMaterialEstudo.setCriador(usuario);

        materialEstudoRepository.save(novoMaterialEstudo);

        outboxService.record("MaterialEstudo", novoMaterialEstudo.getId().toString(), "ArquivoParaSubir",
                comandoUpload(conteudo, BucketSupabase.MATERIAL_CONTEUDO, novoMaterialEstudo.getConteudo()),
                Broker.RABBITMQ, FILA_UPLOAD);

        outboxService.record("MaterialEstudo", novoMaterialEstudo.getId().toString(), "MaterialEstudoCadastrado",
                materialEstudoMapper.materialEstudoParaDocument(novoMaterialEstudo), Broker.KAFKA, TOPICO);

        return materialEstudoMapper.materialEstudoParaResponse(novoMaterialEstudo);

    }

    @Override
    public MaterialEstudoResponse atualizar(AtualizarMaterialEstudoRequest request,
                                            MultipartFile conteudo,
                                            Usuario usuario) throws IOException {

        MaterialEstudo material = materialEstudoRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material de estudo não encontrado"));

        verificarPermissao(material, usuario);

        material.setTemas(temaRepository.findAllById(request.getTemas()));

        material.setCursos(cursoRepository.findAllById(request.getCursos()));

        materialEstudoMapper.atualizarRequestParaMaterialEstudo(request, material);

        if (conteudo != null && !conteudo.isEmpty()) {
            outboxService.record("MaterialEstudo", material.getId().toString(), "ArquivoParaSubir",
                    comandoUpload(conteudo, BucketSupabase.MATERIAL_CONTEUDO, material.getConteudo()),
                    Broker.RABBITMQ, FILA_UPLOAD);
        }

        materialEstudoRepository.save(material);

        outboxService.record("MaterialEstudo", material.getId().toString(), "MaterialEstudoAtualizado",
                materialEstudoMapper.materialEstudoParaDocument(material), Broker.KAFKA, TOPICO);

        return materialEstudoMapper.materialEstudoParaResponse(material);

    }

    @Override
    public void deletar(UUID id, Usuario usuario) {

        MaterialEstudo material = materialEstudoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material de estudo não encontrado"));

        verificarPermissao(material, usuario);

        material.setAtivo(false);

        materialEstudoRepository.save(material);

        outboxService.record("MaterialEstudo", material.getId().toString(), "MaterialEstudoExcluido",
                materialEstudoMapper.materialEstudoParaDocument(material), Broker.KAFKA, TOPICO);

        outboxService.record("MaterialEstudo", material.getId().toString(), "ArquivoParaExcluir",
                new DeletarArquivoCommand(BucketSupabase.MATERIAL_CONTEUDO, material.getConteudo()),
                Broker.RABBITMQ, FILA_DELETE);

    }

    private UploadArquivoCommand comandoUpload(MultipartFile arquivo, BucketSupabase bucket, String nome)
            throws IOException {

        return new UploadArquivoCommand(bucket, nome, arquivo.getContentType(),
                Base64.getEncoder().encodeToString(arquivo.getBytes()));

    }

    private void verificarPermissao(MaterialEstudo material, Usuario usuario) {

        if (usuario.getRole() == Role.BIBLIOTECARIO) {
            return;
        }

        if (material.getCriador() == null) {
            throw new AccessDeniedException(
                    "Este material não possui criador identificado. Solicite ao bibliotecário.");
        }

        if (!material.getCriador().getId().equals(usuario.getId())) {
            throw new AccessDeniedException(
                    "Você só pode alterar materiais de estudo que você mesmo cadastrou.");
        }
    }
}
