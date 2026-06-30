package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.repository.LivroDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LivroEventListener {

    private final LivroDocumentRepository livroDocumentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposCadastrarLivro(LivroCadastroEvent livroCadastroEvent) {

        livroDocumentRepository.save(
                livroCadastroEvent.getLivroDocument());

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposAtualizarLivro(LivroAtualizarEvent livroAtualizarEvent) {

        livroDocumentRepository.save(
                        livroAtualizarEvent.getLivroDocument());

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposDeletarLivro(LivroDeletarEvent livroDeletarEvent) {

        livroDocumentRepository.delete(
                livroDeletarEvent.getLivroDocument());

    }

}
