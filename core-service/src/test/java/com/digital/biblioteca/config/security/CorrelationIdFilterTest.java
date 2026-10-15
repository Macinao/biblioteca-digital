package com.digital.biblioteca.config.security;

import com.digital.smartteca.common.observability.CorrelationId;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void repassaOIdDoGatewayParaOMdcEParaAResposta() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "corr-do-gateway");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcDentroDaCadeia = new AtomicReference<>();
        FilterChain chain = (req, res) -> mdcDentroDaCadeia.set(MDC.get(CorrelationId.MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(mdcDentroDaCadeia.get()).isEqualTo("corr-do-gateway");
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("corr-do-gateway");
    }

    @Test
    void geraUmIdQuandoARequisicaoNaoTrazHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationId.HEADER)).isNotBlank();
    }

    @Test
    void removeOIdDoMdcDepoisDaRequisicao() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "abc");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void removeOIdDoMdcMesmoQuandoACadeiaLancaExcecao() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "abc");
        FilterChain quebrada = (req, res) -> {
            throw new IllegalStateException("erro no controller");
        };

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), quebrada))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }
}
