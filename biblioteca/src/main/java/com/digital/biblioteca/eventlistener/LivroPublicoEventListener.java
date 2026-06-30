package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.mapper.LivroPublicoMapper;
import com.digital.biblioteca.repository.LivroPublicoDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class LivroPublicoEventListener {

    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final LivroPublicoMapper livroPublicoMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposCadastrarLivroPublico(LivroPublicoCadastroEvent livroPublicoCadastroEvent) {

        livroPublicoDocumentRepository.save(
                livroPublicoMapper.livroPublicoParaDocument(
                        livroPublicoCadastroEvent.getLivroPublico()));

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposAtualizarLivroPublico(LivroPublicoAtualizarEvent livroPublicoAtualizarEvent) {

        livroPublicoDocumentRepository.save(livroPublicoAtualizarEvent
                .getLivroPublicoDocument());

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposDeletarLivroPublico(LivroPublicoDeletarEvent livroPublicoDeletarEvent) {

        livroPublicoDocumentRepository.delete(livroPublicoDeletarEvent
                .getLivroPublicoDocument());

    }
}
