package com.digital.biblioteca.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Entity
public class LivroPublico extends MaterialAcademico {

    @ManyToMany
    @JoinTable(
            name = "livro_publico_curso",
            joinColumns = @JoinColumn(name = "livro_publico_id"),
            inverseJoinColumns = @JoinColumn(name = "curso_id")
    )
    private List<Curso> cursos;

    private LocalDateTime dataPublicacao;

    @Column(length = 500)
    private String capa;

    @Column(length = 500)
    private String conteudo;

}
