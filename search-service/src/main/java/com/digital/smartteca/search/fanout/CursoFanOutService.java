package com.digital.smartteca.search.fanout;

import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Livro, LivroPublico, Tcc e MaterialEstudo guardam uma COPIA embutida do
 * Curso (pra nao precisar de um join na hora de exibir a lista). Isso e
 * bom pra leitura, mas significa que renomear um Curso deixa essas copias
 * desatualizadas se ninguem propagar a mudanca — problema que nem existia
 * de verdade no codigo antigo (o CursoEventListener fazia um fan-out
 * manual, sem batching). Aqui isso vira uma consulta (find by curso id)
 * + atualizacao em memoria + saveAll em lote por indice.
 *
 * So entra em jogo em CursoAtualizado: CursoCadastrado nao tem quem
 * referenciar ainda, e CursoExcluido nao muda nada que a copia guarda
 * (o "nome" continua valido — o Curso so fica inativo no Postgres, campo
 * que nem existe no CursoDocument).
 */
@Service
@RequiredArgsConstructor
public class CursoFanOutService {

    private final LivroDocumentRepository livroDocumentRepository;
    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final TccDocumentRepository tccDocumentRepository;
    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;

    public void propagarAtualizacao(CursoDocument cursoAtualizado) {

        String cursoId = cursoAtualizado.getId();

        List<LivroDocument> livros = livroDocumentRepository.findByCursosId(cursoId);
        livros.forEach(livro -> substituir(livro.getCursos(), cursoAtualizado));
        livroDocumentRepository.saveAll(livros);

        List<LivroPublicoDocument> livrosPublicos = livroPublicoDocumentRepository.findByCursosId(cursoId);
        livrosPublicos.forEach(livro -> substituir(livro.getCursos(), cursoAtualizado));
        livroPublicoDocumentRepository.saveAll(livrosPublicos);

        List<MaterialEstudoDocument> materiais = materialEstudoDocumentRepository.findByCursosId(cursoId);
        materiais.forEach(material -> substituir(material.getCursos(), cursoAtualizado));
        materialEstudoDocumentRepository.saveAll(materiais);

        // Tcc guarda um Curso so (singular), nao uma lista — reatribui direto.
        List<TccDocument> tccs = tccDocumentRepository.findByCursoId(cursoId);
        tccs.forEach(tcc -> tcc.setCurso(cursoAtualizado));
        tccDocumentRepository.saveAll(tccs);

    }

    private void substituir(List<CursoDocument> cursos, CursoDocument atualizado) {

        for (int i = 0; i < cursos.size(); i++) {
            if (cursos.get(i).getId().equals(atualizado.getId())) {
                cursos.set(i, atualizado);
            }
        }

    }
}
