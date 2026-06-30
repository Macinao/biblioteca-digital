package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.enums.BucketSupabase;
import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.eventlistener.*;
import com.digital.biblioteca.mapper.MaterialEstudoMapper;
import com.digital.biblioteca.model.MaterialEstudo;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.*;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.response.MaterialEstudoResponse;
import com.digital.biblioteca.service.MaterialEstudoService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MaterialEstudoServiceImpl implements MaterialEstudoService {

    private final MaterialEstudoRepository materialEstudoRepository;
    private final MaterialEstudoMapper materialEstudoMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TemaRepository temaRepository;
    private final CursoRepository cursoRepository;
    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;

    @Override
    public MaterialEstudoResponse cadastrar(CadastrarMaterialEstudoRequest request,
                                            MultipartFile conteudo,
                                            Usuario usuario) {

        MaterialEstudo novoMaterialEstudo = materialEstudoRepository.save(
                materialEstudoMapper.cadastroRequestParaMaterialEstudo(request));

        novoMaterialEstudo.setTemas(temaRepository.findAllById(request.getTemas()));

        novoMaterialEstudo.setCursos(cursoRepository.findAllById(request.getCursos()));

        novoMaterialEstudo.setConteudo(novoMaterialEstudo.getId().toString());

        novoMaterialEstudo.setCriador(usuario);

        materialEstudoRepository.save(novoMaterialEstudo);

        eventPublisher.publishEvent(new SubirEvent(conteudo,
                BucketSupabase.MATERIAL_CONTEUDO, novoMaterialEstudo.getConteudo()));

        eventPublisher.publishEvent(new MaterialEstudoCadastrarEvent(
                materialEstudoMapper.materialEstudoParaDocument(novoMaterialEstudo)));

        return materialEstudoMapper.materialEstudoParaResponse(novoMaterialEstudo);

    }

    @Override
    public MaterialEstudoResponse atualizar(AtualizarMaterialEstudoRequest request,
                                            MultipartFile conteudo,
                                            Usuario usuario) {

        MaterialEstudo material = materialEstudoRepository.findByIdAndAtivoTrue(request.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material de estudo não encontrado"));

        verificarPermissao(material, usuario);

        material.setTemas(temaRepository.findAllById(request.getTemas()));

        material.setCursos(cursoRepository.findAllById(request.getCursos()));

        materialEstudoMapper.atualizarRequestParaMaterialEstudo(request, material);

        if (conteudo != null && !conteudo.isEmpty()) {
            eventPublisher.publishEvent(new SubirEvent(conteudo,
                    BucketSupabase.MATERIAL_CONTEUDO, material.getConteudo()));
        }

        materialEstudoRepository.save(material);

        eventPublisher.publishEvent(new MaterialEstudoAtualizarEvent(
                materialEstudoMapper.materialEstudoParaDocument(material)));

        return materialEstudoMapper.materialEstudoParaResponse(material);

    }

    @Override
    public List<MaterialEstudoResponse> listar() {

        return materialEstudoMapper.listaDocumentParaResponse(
                materialEstudoDocumentRepository.findAll());

    }

    @Override
    public void deletar(UUID id, Usuario usuario) {

        MaterialEstudo material = materialEstudoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material de estudo não encontrado"));

        verificarPermissao(material, usuario);

        material.setAtivo(false);

        materialEstudoRepository.save(material);

        eventPublisher.publishEvent(new MaterialEstudoDeletarEvent(
                materialEstudoMapper.materialEstudoParaDocument(material)));

        eventPublisher.publishEvent(new DeletarArquivoEvent(
                material.getConteudo(), BucketSupabase.MATERIAL_CONTEUDO));

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
