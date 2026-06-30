package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.repository.TccDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TccEventListener {

    private final TccDocumentRepository tccDocumentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposCadastrarTcc(TccCadastrarEvent cadastrarEvent) {

        tccDocumentRepository.save(
                cadastrarEvent.getTccDocument());

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposAtualizarTcc(TccAtualizarEvent tccAtualizarEvent) {

        tccDocumentRepository.save(
                tccAtualizarEvent.getTccDocument());

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposDeletarTcc(TccDeletarEvent tccDeletarEvent) {

        tccDocumentRepository.delete(
                tccDeletarEvent.getTccDocument());

    }
}
