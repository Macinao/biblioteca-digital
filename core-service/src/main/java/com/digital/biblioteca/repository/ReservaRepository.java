package com.digital.biblioteca.repository;

import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.biblioteca.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservaRepository extends JpaRepository<Reserva, UUID> {

    Optional<Reserva> findByIdAndAtivoTrue(UUID id);

    boolean existsByLivroIdAndAtivoTrue(UUID livroId);

    List<Reserva> findByAtivoTrueAndStatusReserva(
            StatusReserva statusReserva);

    List<Reserva> findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(
            StatusReserva statusReserva,
            LocalDateTime data);

    long countByUsuarioMatriculaAndAtivoTrue(String matricula);

}
