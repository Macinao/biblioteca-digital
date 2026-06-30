package com.digital.biblioteca.model;

import com.digital.biblioteca.enums.StatusReserva;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Setter
@Getter
@Entity
public class Reserva extends EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Livro livro;

    @Enumerated(EnumType.STRING)
    private StatusReserva statusReserva;

    private LocalDateTime dataDevolucao;

}
