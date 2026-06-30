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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Livro l SET l.quantidade = l.quantidade - 1 " +
            "WHERE l.id = :id AND l.quantidade > 0 AND l.ativo = true")
    int decrementarQuantidade(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Livro l SET l.quantidade = l.quantidade + 1 " +
            "WHERE l.id = :id AND l.ativo = true")
    int incrementarQuantidade(@Param("id") UUID id);

}
