package com.digital.smartteca.common.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CorrelationIdTest {

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void resolveMantemOIdQuandoVemPreenchido() {
        assertEquals("abc-123", CorrelationId.resolve("abc-123"));
    }

    @Test
    void resolveGeraUuidQuandoNuloOuEmBranco() {
        for (String vazio : new String[]{null, "", "   "}) {
            String gerado = CorrelationId.resolve(vazio);
            assertDoesNotThrow(() -> UUID.fromString(gerado));
        }
    }

    @Test
    void resolveGeraIdsDiferentesACadaChamada() {
        assertNotEquals(CorrelationId.resolve(null), CorrelationId.resolve(null));
    }

    @Test
    void openColocaOIdNoMdcEFechaRemove() {
        try (CorrelationId.Scope ignored = CorrelationId.open("req-1")) {
            assertEquals("req-1", MDC.get(CorrelationId.MDC_KEY));
        }
        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }

    @Test
    void openSemIdGeraUmNoMdc() {
        try (CorrelationId.Scope ignored = CorrelationId.open(null)) {
            assertDoesNotThrow(() -> UUID.fromString(MDC.get(CorrelationId.MDC_KEY)));
        }
    }

    @Test
    void idNaoVazaDoMdcQuandoOEscopoTerminaComExcecao() {
        assertThrows(IllegalStateException.class, () -> {
            try (CorrelationId.Scope ignored = CorrelationId.open("req-2")) {
                throw new IllegalStateException("falha no consumer");
            }
        });
        assertNull(MDC.get(CorrelationId.MDC_KEY));
    }

    @Test
    void headerEChaveDoMdcSaoOsContratosEsperados() {
        assertEquals("X-Correlation-Id", CorrelationId.HEADER);
        assertEquals("correlationId", CorrelationId.MDC_KEY);
    }
}
