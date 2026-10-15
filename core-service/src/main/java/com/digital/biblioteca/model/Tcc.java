package com.digital.biblioteca.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
public class Tcc extends MaterialAcademico {

    @ManyToOne
    @JoinColumn(name = "curso_id")
    private Curso curso;

    private LocalDateTime dataPublicacao;

    @Column(length = 500)
    private String conteudo;

}
