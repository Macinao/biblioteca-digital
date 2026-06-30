package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.*;
import com.digital.biblioteca.mapper.CursoMapper;
import com.digital.biblioteca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CursoEventListener {

    private final CursoDocumentRepository cursoDocumentRepository;
    private final CursoMapper cursoMapper;
    private final LivroDocumentRepository livroDocumentRepository;
    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;
    private final TccDocumentRepository tccDocumentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposCadastroCurso(CursoCadastroEvent cursoCadastroEvent) {

        cursoDocumentRepository.save(
                cursoMapper.cursoParaDocument(
                        cursoCadastroEvent.getCurso()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposAtualizarCurso(CursoAtualizarEvent cursoAtualizarEvent) {

        cursoDocumentRepository.save(cursoAtualizarEvent.getCurso());

        List<LivroDocument> livros = livroDocumentRepository
                .findByCursosId(cursoAtualizarEvent.getCurso().getId());

        for (LivroDocument livroDocument : livros) {

            List<CursoDocument> cursos = livroDocument.getCursos();

            for (int i = 0; i < cursos.size(); i++) {

                if (cursos.get(i).getId().equals(cursoAtualizarEvent.getCurso().getId())) {

                    cursos.set(i, cursoAtualizarEvent.getCurso());

                }

            }

            livroDocumentRepository.save(livroDocument);
        }

        List<LivroPublicoDocument> livrosPublicos = livroPublicoDocumentRepository
                .findByCursosId(cursoAtualizarEvent.getCurso().getId());

        for (LivroPublicoDocument livroPublicoDocument : livrosPublicos) {

            List<CursoDocument> cursos = livroPublicoDocument.getCursos();

            for (int i = 0; i < cursos.size(); i++) {

                if (cursos.get(i).getId().equals(cursoAtualizarEvent.getCurso().getId())) {

                    cursos.set(i, cursoAtualizarEvent.getCurso());

                }

            }

            livroPublicoDocumentRepository.save(livroPublicoDocument);
        }

        List<MaterialEstudoDocument> materiaisEstudo = materialEstudoDocumentRepository
                .findByCursosId(cursoAtualizarEvent.getCurso().getId());

        for (MaterialEstudoDocument materialEstudoDocument : materiaisEstudo) {

            List<CursoDocument> cursos = materialEstudoDocument.getCursos();

            for (int i = 0; i < cursos.size(); i++) {

                if (cursos.get(i).getId().equals(cursoAtualizarEvent.getCurso().getId())) {

                    cursos.set(i, cursoAtualizarEvent.getCurso());
                }

            }

            materialEstudoDocumentRepository.save(materialEstudoDocument);
        }

        List<TccDocument> tccs = tccDocumentRepository
                .findByCursoId(cursoAtualizarEvent.getCurso().getId());

        for (TccDocument tccDocument : tccs) {

            if (tccDocument.getCurso().getId().equals(cursoAtualizarEvent.getCurso().getId())) {

                tccDocument.setCurso(cursoAtualizarEvent.getCurso());

            }

            tccDocumentRepository.save(tccDocument);

        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposExcluirCurso(CursoExcluirEvent cursoExcluirEvent) {

        cursoDocumentRepository.delete(cursoExcluirEvent.getCurso());

        List<LivroDocument> livros = livroDocumentRepository
                .findByCursosId(cursoExcluirEvent.getCurso().getId());

        for (LivroDocument livroDocument : livros) {

            List<CursoDocument> cursos = livroDocument.getCursos();

            for (int i = cursos.size() - 1; i >= 0; i--) {
                if (cursos.get(i).getId().equals(cursoExcluirEvent.getCurso().getId())) {
                    cursos.remove(i);
                }
            }

            livroDocumentRepository.save(livroDocument);
        }

        List<LivroPublicoDocument> livrosPublicos = livroPublicoDocumentRepository
                .findByCursosId(cursoExcluirEvent.getCurso().getId());

        for (LivroPublicoDocument livroPublicoDocument : livrosPublicos) {

            List<CursoDocument> cursos = livroPublicoDocument.getCursos();

            for (int i = cursos.size() - 1; i >= 0; i--) {
                if (cursos.get(i).getId().equals(cursoExcluirEvent.getCurso().getId())) {
                    cursos.remove(i);
                }
            }

            livroPublicoDocumentRepository.save(livroPublicoDocument);
        }

        List<MaterialEstudoDocument> materiaisEstudo = materialEstudoDocumentRepository
                .findByCursosId(cursoExcluirEvent.getCurso().getId());

        for (MaterialEstudoDocument materialEstudoDocument : materiaisEstudo) {

            List<CursoDocument> cursos = materialEstudoDocument.getCursos();

            for (int i = cursos.size() - 1; i >= 0; i--) {
                if (cursos.get(i).getId().equals(cursoExcluirEvent.getCurso().getId())) {
                    cursos.remove(i);
                }
            }

            materialEstudoDocumentRepository.save(materialEstudoDocument);
        }

        List<TccDocument> tccs = tccDocumentRepository
                .findByCursoId(cursoExcluirEvent.getCurso().getId());

        for (TccDocument tccDocument : tccs) {

            tccDocument.setCurso(null);

            tccDocumentRepository.save(tccDocument);
        }
    }
}
