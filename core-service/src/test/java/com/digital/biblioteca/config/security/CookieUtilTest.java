package com.digital.biblioteca.config.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilTest {

    CookieProperties properties;
    CookieUtil cookieUtil;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        properties = new CookieProperties();
        cookieUtil = new CookieUtil(properties);
        response = new MockHttpServletResponse();
    }

    @Test
    void accessCookieEHttpOnlyComPathRaizEDuracaoDeTresHoras() {
        cookieUtil.setAccessCookie(response, "tok-access");

        String header = response.getHeader("Set-Cookie");
        assertThat(header).startsWith("smartteca_access=tok-access");
        assertThat(header).contains("HttpOnly", "Path=/", "Max-Age=10800", "SameSite=Lax");
        assertThat(header).doesNotContain("Secure");
    }

    @Test
    void refreshCookieFicaRestritoAoPathDeAuthComDuracaoDeSeteDias() {
        cookieUtil.setRefreshCookie(response, "tok-refresh");

        String header = response.getHeader("Set-Cookie");
        assertThat(header).startsWith("smartteca_refresh=tok-refresh");
        assertThat(header).contains("HttpOnly", "Path=/api/v1/auth", "Max-Age=604800");
    }

    @Test
    void propriedadeSecureLigaOAtributoSecure() {
        properties.setSecure(true);

        cookieUtil.setAccessCookie(response, "t");

        assertThat(response.getHeader("Set-Cookie")).contains("Secure");
    }

    @Test
    void sameSiteConfiguradoEAplicado() {
        properties.setSameSite("Strict");

        cookieUtil.setAccessCookie(response, "t");

        assertThat(response.getHeader("Set-Cookie")).contains("SameSite=Strict");
    }

    @Test
    void domainSoApareceQuandoConfigurado() {
        cookieUtil.setAccessCookie(response, "t");
        assertThat(response.getHeader("Set-Cookie")).doesNotContain("Domain");

        MockHttpServletResponse outra = new MockHttpServletResponse();
        properties.setDomain("smartteca.com.br");
        cookieUtil.setAccessCookie(outra, "t");
        assertThat(outra.getHeader("Set-Cookie")).contains("Domain=smartteca.com.br");
    }

    @Test
    void domainNuloOuEmBrancoNaoGeraAtributo() {
        properties.setDomain(null);
        cookieUtil.setAccessCookie(response, "t");
        assertThat(response.getHeader("Set-Cookie")).doesNotContain("Domain");

        MockHttpServletResponse emBranco = new MockHttpServletResponse();
        properties.setDomain("   ");
        cookieUtil.setAccessCookie(emBranco, "t");
        assertThat(emBranco.getHeader("Set-Cookie")).doesNotContain("Domain");
    }

    @Test
    void clearAuthCookiesExpiraOsDoisCookiesNosPathsCorretos() {
        cookieUtil.clearAuthCookies(response);

        List<String> headers = response.getHeaders("Set-Cookie");
        assertThat(headers).hasSize(2);
        assertThat(headers.get(0)).startsWith("smartteca_access=;").contains("Max-Age=0", "Path=/");
        assertThat(headers.get(1)).startsWith("smartteca_refresh=;").contains("Max-Age=0", "Path=/api/v1/auth");
    }

    @Test
    void readCookieDevolveOValorQuandoExiste() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("outro", "x"), new Cookie("smartteca_access", "abc"));

        assertThat(cookieUtil.readCookie(request, "smartteca_access")).contains("abc");
    }

    @Test
    void readCookieDevolveVazioSemCookies() {
        assertThat(cookieUtil.readCookie(new MockHttpServletRequest(), "smartteca_access")).isEmpty();
    }

    @Test
    void readCookieDevolveVazioQuandoNaoHaCookieComONome() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("outro", "x"));

        assertThat(cookieUtil.readCookie(request, "smartteca_access")).isEmpty();
    }

    @Test
    void readCookieIgnoraValorEmBranco() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("smartteca_access", "   "));

        assertThat(cookieUtil.readCookie(request, "smartteca_access")).isEmpty();
    }
}
