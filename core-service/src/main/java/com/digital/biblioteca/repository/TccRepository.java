package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.Tcc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TccRepository extends JpaRepository<Tcc, UUID> {

    Optional<Tcc> findByIdAndAtivoTrue(UUID id);

}
