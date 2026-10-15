package com.digital.smartteca.chatbot.security;

import com.digital.smartteca.common.observability.CorrelationId;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SegurancaChatbotTest {

    final GatewayHeaderAuthFilter authFilter = new GatewayHeaderAuthFilter();
    final CorrelationIdFilter correlationFilter = new CorrelationIdFilter();

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    private Authentication autenticacaoAposFiltrar(MockHttpServletRequest request) throws Exception {
        AtomicReference<Authentication> naCadeia = new AtomicReference<>();
        FilterChain chain = (req, res) -> naCadeia.set(SecurityContextHolder.getContext().getAuthentication());
        authFilter.doFilter(request, new MockHttpServletResponse(), chain);
        return naCadeia.get();
    }

    private static MockHttpServletRequest comHeaders(String matricula, String roles) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (matricula != null) {
            request.addHeader("X-User-Matricula", matricula);
        }
        if (roles != null) {
            request.addHeader("X-User-Roles", roles);
        }
        return request;
    }

    @Test
    void headersDoGatewayViramAutenticacaoComMatriculaComoPrincipalEPapeisPrefixados() throws Exception {
        Authentication auth = autenticacaoAposFiltrar(comHeaders("2023001", "PROFESSOR, ALUNO"));

        assertThat(auth.getName()).isEqualTo("2023001");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactlyInAnyOrder("ROLE_PROFESSOR", "ROLE_ALUNO");
    }

    @Test
    void papeisVaziosNaListaSaoIgnorados() throws Exception {
        Authentication auth = autenticacaoAposFiltrar(comHeaders("2023001", "ALUNO,, ,"));

        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_ALUNO");
    }

    @Test
    void semHeadersOuComHeadersEmBrancoNaoAutentica() throws Exception {
        assertThat(autenticacaoAposFiltrar(comHeaders(null, null))).isNull();
        assertThat(autenticacaoAposFiltrar(comHeaders("2023001", null))).isNull();
        assertThat(autenticacaoAposFiltrar(comHeaders(null, "ALUNO"))).isNull();
        assertThat(autenticacaoAposFiltrar(comHeaders("  ", "ALUNO"))).isNull();
        assertThat(autenticacaoAposFiltrar(comHeaders("2023001", "  "))).isNull();
    }

    @Test
    void correlationIdDoGatewayVaiParaOMdcEParaARespostaESaiDoMdcDepois() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "corr-chat");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdc = new AtomicReference<>();

        correlationFilter.doFilter(request, response, (req, res) -> mdc.set(MDC.get(CorrelationId.MDC_KEY)));

        assertThat(mdc.get()).isEqualTo("corr-chat");
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("corr-chat");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void semCorrelationIdGeraUmENaoVazaDoMdcMesmoComExcecao() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> correlationFilter.doFilter(new MockHttpServletRequest(), response, (req, res) -> {
            throw new IllegalStateException("erro");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(response.getHeader(CorrelationId.HEADER)).isNotBlank();
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }
}
