package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    boolean existsByEmail(String email);

}
