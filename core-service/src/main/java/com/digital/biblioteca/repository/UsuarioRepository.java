package com.digital.biblioteca.repository;

import com.digital.biblioteca.model.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByMatricula(String matricula);

    // SELECT ... FOR UPDATE: quem reserva segura a linha do usuário até o commit, então duas reservas
    // simultâneas da mesma pessoa se enfileiram e a segunda enxerga a contagem já atualizada.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findWithLockByMatricula(String matricula);

    boolean existsByMatricula(String matricula);

    boolean existsByEmail(String email);

}
