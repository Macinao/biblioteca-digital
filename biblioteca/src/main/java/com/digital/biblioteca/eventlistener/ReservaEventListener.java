package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.repository.ReservaDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ReservaEventListener {

    private final ReservaDocumentRepository reservaDocumentRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposReservar(ReservarEvent reservarEvent) throws IOException {

        reservaDocumentRepository.save(
                reservarEvent.getReservaDocument()
        );

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposDevolver(DevolverEvent devolverEvent) throws IOException {

        reservaDocumentRepository.delete(
                devolverEvent.getReservaDocument()
        );

    }
}
