package com.digital.biblioteca.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * FOR UPDATE trava as linhas selecionadas; SKIP LOCKED faz uma segunda
     * instancia do relay (se um dia existir mais de uma) pular linhas que
     * outra ja esteja processando, em vez de esperar ou publicar em
     * duplicidade.
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE status = 'PENDING'
            ORDER BY data_criacao ASC
            LIMIT :limite
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> buscarPendentes(@Param("limite") int limite);

}
