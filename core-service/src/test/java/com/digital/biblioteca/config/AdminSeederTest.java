package com.digital.biblioteca.config;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminSeederTest {

    UsuarioRepository repository = mock(UsuarioRepository.class);
    PasswordEncoder encoder = mock(PasswordEncoder.class);

    @Test
    void criaOBibliotecarioInicialComSenhaHasheadaQuandoAMatriculaNaoExiste() {
        when(repository.existsByMatricula("ADMIN001")).thenReturn(false);
        when(encoder.encode("Senha1234")).thenReturn("hash");

        new AdminSeeder(repository, encoder, "ADMIN001", "Senha1234").run(null);

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).save(salvo.capture());
        assertThat(salvo.getValue().getMatricula()).isEqualTo("ADMIN001");
        assertThat(salvo.getValue().getSenha()).isEqualTo("hash");
        assertThat(salvo.getValue().getRole()).isEqualTo(Role.BIBLIOTECARIO);
    }

    @Test
    void naoCriaNemSobrescreveSeAMatriculaJaExiste() {
        when(repository.existsByMatricula("ADMIN001")).thenReturn(true);

        new AdminSeeder(repository, encoder, "ADMIN001", "Senha1234").run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void naoCriaNadaSemAsVariaveisDeAmbiente() {
        new AdminSeeder(repository, encoder, "", "").run(null);
        new AdminSeeder(repository, encoder, "ADMIN001", "").run(null);

        verify(repository, never()).save(any());
    }
}
