package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.MaterialEstudo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MaterialEstudoRepository extends JpaRepository<MaterialEstudo, UUID> {

    Optional<MaterialEstudo> findByIdAndAtivoTrue(UUID id);

}
