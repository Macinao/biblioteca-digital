package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsuarioDetailsServiceImplTest {

    UsuarioRepository repository = mock(UsuarioRepository.class);
    UsuarioDetailsServiceImpl service = new UsuarioDetailsServiceImpl(repository);

    @Test
    void devolveOUsuarioEncontradoPelaMatricula() {
        Usuario usuario = new Usuario();
        usuario.setMatricula("2023001");
        when(repository.findByMatricula("2023001")).thenReturn(Optional.of(usuario));

        UserDetails details = service.loadUserByUsername("2023001");

        assertThat(details).isSameAs(usuario);
        assertThat(details.getUsername()).isEqualTo("2023001");
    }

    @Test
    void lancaUsernameNotFoundComAMatriculaNaMensagemQuandoNaoExiste() {
        when(repository.findByMatricula("0000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("0000000"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("0000000");
    }
}
