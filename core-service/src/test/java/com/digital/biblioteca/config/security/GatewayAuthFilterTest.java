package com.digital.biblioteca.config.security;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.service.implement.UsuarioDetailsServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

class GatewayAuthFilterTest {

    UsuarioDetailsServiceImpl usuarioDetailsService = mock(UsuarioDetailsServiceImpl.class);
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    GatewayAuthFilter filter;

    MockHttpServletRequest request;
    MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new GatewayAuthFilter(usuarioDetailsService, objectMapper);
        request = new MockHttpServletRequest("GET", "/api/v1/livro");
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void semHeaderSegueAdianteSemAutenticarNemConsultarOBanco() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(usuarioDetailsService);
    }

    @Test
    void headerEmBrancoSeTrataComoAusente() throws Exception {
        request.addHeader("X-User-Matricula", "   ");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(usuarioDetailsService);
    }

    @Test
    void headerValidoAutenticaComOUsuarioDoBancoESegueAdiante() throws Exception {
        Usuario usuario = usuario("2023001", Role.PROFESSOR);
        when(usuarioDetailsService.loadUserByUsername("2023001")).thenReturn(usuario);
        request.addHeader("X-User-Matricula", "2023001");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isSameAs(usuario);
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_PROFESSOR", "ROLE_ALUNO");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void usuarioInexistenteRespondeUnauthorizedEmJsonENaoSegueAdiante() throws Exception {
        when(usuarioDetailsService.loadUserByUsername("9999999"))
                .thenThrow(new UsernameNotFoundException("Usuário não encontrado com matrícula: 9999999"));
        request.addHeader("X-User-Matricula", "9999999");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        JsonNode corpo = objectMapper.readTree(response.getContentAsString());
        assertThat(corpo.get("statusCode").asInt()).isEqualTo(401);
        assertThat(corpo.get("status").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(corpo.get("message").asText()).isEqualTo("Usuário do header não existe mais");
        assertThat(corpo.get("path").asText()).isEqualTo("/api/v1/livro");
        verify(chain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void contaDesativadaRespondeUnauthorizedMesmoComTokenAindaValidoENaoSegueAdiante() throws Exception {
        Usuario desativado = usuario("2023001", Role.ALUNO);
        desativado.setAtivo(false);
        when(usuarioDetailsService.loadUserByUsername("2023001")).thenReturn(desativado);
        request.addHeader("X-User-Matricula", "2023001");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(objectMapper.readTree(response.getContentAsString()).get("message").asText()).isEqualTo("Conta desativada");
        verify(chain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private static Usuario usuario(String matricula, Role role) {
        Usuario usuario = new Usuario();
        usuario.setMatricula(matricula);
        usuario.setRole(role);
        usuario.setAtivo(true);
        return usuario;
    }
}
