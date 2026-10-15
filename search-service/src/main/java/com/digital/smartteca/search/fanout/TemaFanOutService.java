package com.digital.smartteca.search.fanout;

import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.document.TemaDocument;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Mesma ideia do CursoFanOutService, mas para Tema — que aparece como
 * lista embutida nos quatro indices (diferente do Curso no Tcc, o campo
 * de temas e sempre List<TemaDocument>, nunca singular).
 */
@Service
@RequiredArgsConstructor
public class TemaFanOutService {

    private final LivroDocumentRepository livroDocumentRepository;
    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final TccDocumentRepository tccDocumentRepository;
    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;

    public void propagarAtualizacao(TemaDocument temaAtualizado) {

        String temaId = temaAtualizado.getId();

        List<LivroDocument> livros = livroDocumentRepository.findByTemasId(temaId);
        livros.forEach(livro -> substituir(livro.getTemas(), temaAtualizado));
        livroDocumentRepository.saveAll(livros);

        List<LivroPublicoDocument> livrosPublicos = livroPublicoDocumentRepository.findByTemasId(temaId);
        livrosPublicos.forEach(livro -> substituir(livro.getTemas(), temaAtualizado));
        livroPublicoDocumentRepository.saveAll(livrosPublicos);

        List<MaterialEstudoDocument> materiais = materialEstudoDocumentRepository.findByTemasId(temaId);
        materiais.forEach(material -> substituir(material.getTemas(), temaAtualizado));
        materialEstudoDocumentRepository.saveAll(materiais);

        List<TccDocument> tccs = tccDocumentRepository.findByTemasId(temaId);
        tccs.forEach(tcc -> substituir(tcc.getTemas(), temaAtualizado));
        tccDocumentRepository.saveAll(tccs);

    }

    private void substituir(List<TemaDocument> temas, TemaDocument atualizado) {

        for (int i = 0; i < temas.size(); i++) {
            if (temas.get(i).getId().equals(atualizado.getId())) {
                temas.set(i, atualizado);
            }
        }

    }
}
