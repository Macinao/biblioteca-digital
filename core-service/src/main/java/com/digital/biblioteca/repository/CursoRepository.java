package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CursoRepository extends JpaRepository<Curso, UUID> {

    boolean existsByNomeIgnoreCaseAndAtivoTrue(String nome);

    boolean existsByNomeIgnoreCaseAndAtivoTrueAndIdNot(String nome, UUID id);

    Optional<Curso> findByIdAndAtivoTrue(UUID id);

}
