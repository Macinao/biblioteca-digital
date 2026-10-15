package com.digital.smartteca.common.security;

import java.util.List;

/**
 * Fonte unica das rotas de negocio que nao exigem usuario autenticado.
 * Usada tanto pelo api-gateway (decide se precisa validar o JWT antes de
 * encaminhar) quanto pelo core-service (decide se o GatewayAuthFilter
 * precisa ter encontrado um usuario pra liberar o endpoint). As duas
 * checagens continuam rodando de forma independente uma da outra — o que
 * fica centralizado aqui e so a LISTA de valores, nao a verificacao em si.
 *
 * Endpoints de infraestrutura de cada servico (actuator, swagger) NAO
 * entram aqui — sao especificos de cada um, nao rotas de negocio
 * compartilhadas entre gateway e core-service.
 */
public final class RotasPublicas {

    private RotasPublicas() {
    }

    public static final List<String> AUTH = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );
}
