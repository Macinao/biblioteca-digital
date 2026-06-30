package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.LivroPublico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LivroPublicoRepository extends JpaRepository<LivroPublico, UUID> {

    Optional<LivroPublico> findByIdAndAtivoTrue(UUID id);

}
