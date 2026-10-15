package com.digital.smartteca.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RotasPublicasTest {

    @Test
    void listaContemExatamenteAsRotasQueNaoExigemLogin() {
        assertEquals(
                java.util.List.of("/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout"),
                RotasPublicas.AUTH);
    }

    @Test
    void todasAsRotasComecamComOPrefixoDaApi() {
        assertTrue(RotasPublicas.AUTH.stream().allMatch(rota -> rota.startsWith("/api/v1/")));
    }

    @Test
    void listaCompartilhadaEImutavel() {
        assertThrows(UnsupportedOperationException.class, () -> RotasPublicas.AUTH.add("/api/v1/livro"));
    }
}
