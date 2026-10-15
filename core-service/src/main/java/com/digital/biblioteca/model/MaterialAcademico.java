package com.digital.biblioteca.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class MaterialAcademico extends EntidadeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 500)
    private String titulo;

    @Column(length = 255)
    private String autor;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @ManyToMany
    @JoinTable(
            name = "material_tema",
            joinColumns = @JoinColumn(name = "material_id"),
            inverseJoinColumns = @JoinColumn(name = "tema_id")
    )
    private List<Tema> temas;

}
