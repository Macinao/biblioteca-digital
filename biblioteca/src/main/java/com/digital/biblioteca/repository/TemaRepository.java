package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.Tema;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TemaRepository extends JpaRepository<Tema, UUID> {

    boolean existsByNomeIgnoreCaseAndAtivoTrue(String nome);

    boolean existsByNomeIgnoreCaseAndAtivoTrueAndIdNot(String nome, UUID id);

    Optional<Tema> findByIdAndAtivoTrue(UUID id);

}
