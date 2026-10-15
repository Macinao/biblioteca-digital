package com.digital.smartteca.common.enums;

/**
 * Compartilhado entre core-service (dono do dado, grava no Postgres) e
 * search-service (le do documento denormalizado no Elasticsearch) — os
 * dois precisam representar exatamente o mesmo conjunto de status, entao
 * fica no modulo common em vez de duplicado.
 */
public enum StatusReserva {
    RESERVADO("RESERVADO"),
    ATRASADO("ATRASADO");

    private final String name;

    StatusReserva(String name) {
        this.name = name;
    }
}
