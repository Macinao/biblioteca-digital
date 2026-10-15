package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LivroRepository extends JpaRepository<Livro, UUID> {

    Optional<Livro> findByIdAndAtivoTrue(UUID id);

    // Nativa de proposito: Livro usa heranca JOINED, e o Hibernate traduz um UPDATE em JPQL numa
    // consulta em duas fases (SELECT dos ids + UPDATE por id) que NAO reconfere "quantidade > 0"
    // depois do lock da linha — reservas simultaneas do ultimo exemplar passavam todas. O UPDATE
    // de tabela unica abaixo e atomico no Postgres. "ativo" vive na tabela-raiz material_academico.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE livro SET quantidade = quantidade - 1 " +
            "WHERE id = :id AND quantidade > 0 " +
            "AND EXISTS (SELECT 1 FROM material_academico m WHERE m.id = livro.id AND m.ativo = true)",
            nativeQuery = true)
    int decrementarQuantidade(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Livro l SET l.quantidade = l.quantidade + 1 " +
            "WHERE l.id = :id AND l.ativo = true")
    int incrementarQuantidade(@Param("id") UUID id);

}
