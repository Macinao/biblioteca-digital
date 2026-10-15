package com.digital.biblioteca.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Entity
public class Livro extends MaterialAcademico {

    @ManyToMany
    @JoinTable(
            name = "livro_curso",
            joinColumns = @JoinColumn(name = "livro_id"),
            inverseJoinColumns = @JoinColumn(name = "curso_id")
    )
    private List<Curso> cursos;

    private LocalDateTime dataPublicacao;

    private Integer quantidade;

    @Column(length = 500)
    private String capa;

}
