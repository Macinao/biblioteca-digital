package com.digital.biblioteca.model;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Setter
@Getter
@MappedSuperclass
public abstract class EntidadeBase {

    private LocalDateTime dataCriacao;

    private LocalDateTime dataAtualizacao;

    private boolean ativo;

    @PrePersist
    public void prePersistir() {
        this.dataCriacao = LocalDateTime.now();
        this.setAtivo(true);
    }

    @PreUpdate
    public void preAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

}
