package com.digital.biblioteca.eventlistener;

import com.digital.biblioteca.repository.MaterialEstudoDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class MateralEstudoEventListener {

    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;

    @Async
    @TransactionalEventListener
    void aposCadastrarMaterialEstudo(MaterialEstudoCadastrarEvent materialEstudoCadastrarEvent){

        materialEstudoDocumentRepository.save(
                materialEstudoCadastrarEvent.getMaterialEstudoDocument());

    }

    @Async
    @TransactionalEventListener
    void aposAtualizarMaterialEstudo(MaterialEstudoAtualizarEvent materialEstudoAtualizarEvent){

        materialEstudoDocumentRepository.save(
                materialEstudoAtualizarEvent.getMaterialEstudoDocument());

    }

    @Async
    @TransactionalEventListener
    void aposDeletarMaterialEstudo(MaterialEstudoDeletarEvent materialEstudoDeletarEvent){

        materialEstudoDocumentRepository.delete(
                materialEstudoDeletarEvent.getMaterialEstudoDocument());

    }
}
