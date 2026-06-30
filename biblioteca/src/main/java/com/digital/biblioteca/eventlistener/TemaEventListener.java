package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.document.*;
import com.digital.biblioteca.mapper.TemaMapper;
import com.digital.biblioteca.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TemaEventListener {

    private final LivroDocumentRepository livroDocumentRepository;
    private final TemaDocumentRepository temaDocumentRepository;
    private final TemaMapper temaMapper;
    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;
    private final TccDocumentRepository tccDocumentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposCadastroTema(TemaCadastroEvent temaCadastroEvent) {

        temaDocumentRepository.save(
                temaMapper.temaParaDocument(
                        temaCadastroEvent.getTema()));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposAtualizarTema(TemaAtualizarEvent temaAtualizarEvent) {

        temaDocumentRepository.save(temaAtualizarEvent.getTema());

        List<LivroDocument> livros = livroDocumentRepository
                .findByTemasId(temaAtualizarEvent.getTema().getId());

        for (LivroDocument livroDocument : livros) {

            List<TemaDocument> temas = livroDocument.getTemas();

            for (int i = 0; i < temas.size(); i++) {

                if (temas.get(i).getId().equals(temaAtualizarEvent.getTema().getId())) {

                    temas.set(i, temaAtualizarEvent.getTema());

                }

            }

            livroDocumentRepository.save(livroDocument);

        }

        List<LivroPublicoDocument> livrosPublicos = livroPublicoDocumentRepository
                .findByTemasId(temaAtualizarEvent.getTema().getId());

        for (LivroPublicoDocument livroPublicoDocument : livrosPublicos) {

            List<TemaDocument> temas = livroPublicoDocument.getTemas();

            for (int i = 0; i < temas.size(); i++) {

                if (temas.get(i).getId().equals(temaAtualizarEvent.getTema().getId())) {

                    temas.set(i, temaAtualizarEvent.getTema());

                }

            }

            livroPublicoDocumentRepository.save(livroPublicoDocument);

        }

        List<MaterialEstudoDocument> materiaisEstudo = materialEstudoDocumentRepository
                .findByTemasId(temaAtualizarEvent.getTema().getId());

        for (MaterialEstudoDocument materialEstudoDocument : materiaisEstudo) {

            List<TemaDocument> temas = materialEstudoDocument.getTemas();

            for (int i = 0; i < temas.size(); i++) {

                if (temas.get(i).getId().equals(temaAtualizarEvent.getTema().getId())) {

                    temas.set(i, temaAtualizarEvent.getTema());

                }

            }

            materialEstudoDocumentRepository.save(materialEstudoDocument);

        }

        List<TccDocument> tccs = tccDocumentRepository
                .findByTemasId(temaAtualizarEvent.getTema().getId());

        for (TccDocument tccDocument : tccs) {

            List<TemaDocument> temas = tccDocument.getTemas();

            for (int i = 0; i < temas.size(); i++) {

                if (temas.get(i).getId().equals(temaAtualizarEvent.getTema().getId())) {

                    temas.set(i, temaAtualizarEvent.getTema());

                }

            }

            tccDocumentRepository.save(tccDocument);

        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposExcluirTema(TemaExcluirEvent temaExcluirEvent) {

        temaDocumentRepository.delete(temaExcluirEvent.getTema());

        List<LivroDocument> livros = livroDocumentRepository
                .findByTemasId(temaExcluirEvent.getTema().getId());

        for (LivroDocument livroDocument : livros) {

            List<TemaDocument> temas = livroDocument.getTemas();

            for (int i = temas.size() - 1; i >= 0; i--) {

                if (temas.get(i).getId().equals(temaExcluirEvent.getTema().getId())) {

                    temas.remove(i);
                }

            }

            livroDocumentRepository.save(livroDocument);
        }

        List<LivroPublicoDocument> livrosPublicos = livroPublicoDocumentRepository
                .findByTemasId(temaExcluirEvent.getTema().getId());

        for (LivroPublicoDocument livroPublicoDocument : livrosPublicos) {

            List<TemaDocument> temas = livroPublicoDocument.getTemas();

            for (int i = temas.size() - 1; i >= 0; i--) {

                if (temas.get(i).getId().equals(temaExcluirEvent.getTema().getId())) {

                    temas.remove(i);
                }

            }

            livroPublicoDocumentRepository.save(livroPublicoDocument);
        }

        List<MaterialEstudoDocument> materiaisEstudo = materialEstudoDocumentRepository
                .findByTemasId(temaExcluirEvent.getTema().getId());

        for (MaterialEstudoDocument materialEstudoDocument : materiaisEstudo) {

            List<TemaDocument> temas = materialEstudoDocument.getTemas();

            for (int i = temas.size() - 1; i >= 0; i--) {

                if (temas.get(i).getId().equals(temaExcluirEvent.getTema().getId())) {

                    temas.remove(i);

                }

            }

            materialEstudoDocumentRepository.save(materialEstudoDocument);
        }

        List<TccDocument> tccs = tccDocumentRepository
                .findByTemasId(temaExcluirEvent.getTema().getId());

        for (TccDocument tccDocument : tccs) {

            List<TemaDocument> temas = tccDocument.getTemas();

            for (int i = temas.size() - 1; i >= 0; i--) {

                if (temas.get(i).getId().equals(temaExcluirEvent.getTema().getId())) {

                    temas.remove(i);

                }

            }

            tccDocumentRepository.save(tccDocument);

        }
    }
}
