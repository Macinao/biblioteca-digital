package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.service.SupabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SupabaseEventListener {

    private final SupabaseService supabaseService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposSubir(SubirEvent subirEvent) throws IOException {

        supabaseService.subir(subirEvent.conteudo,
                subirEvent.bucket, subirEvent.nome);

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposDeletarCapa(DeletarArquivoEvent deletarArquivoEvent) throws IOException {

        supabaseService.deletar(deletarArquivoEvent.getArquivo(),
                deletarArquivoEvent.getBucketArquivo());

    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aposDeletarConteudoCapa(DeletarContudoCapaEvent deletarContudoCapaEvent) throws IOException {

        supabaseService.deletar(deletarContudoCapaEvent.getConteudo(),
                deletarContudoCapaEvent.getBucketConteudo());

        supabaseService.deletar(deletarContudoCapaEvent.getCapa(),
                deletarContudoCapaEvent.getBucketCapa());

    }
}
