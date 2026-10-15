package com.digital.biblioteca.service.implement;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.digital.biblioteca.config.security.TokenService;
import com.digital.biblioteca.mapper.UsuarioMapper;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.UsuarioRepository;
import com.digital.biblioteca.request.CadastrarUsuarioRequest;
import com.digital.biblioteca.request.LoginUsuarioRequest;
import com.digital.biblioteca.response.TokenAcessoResponse;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock TokenService tokenService;
    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioMapper usuarioMapper;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthServiceImpl service;

    private static LoginUsuarioRequest login(String matricula, String senha) {
        LoginUsuarioRequest request = new LoginUsuarioRequest();
        request.setMatricula(matricula);
        request.setSenha(senha);
        return request;
    }

    private static CadastrarUsuarioRequest cadastro() {
        CadastrarUsuarioRequest request = new CadastrarUsuarioRequest();
        request.setNome("Maria Souza");
        request.setMatricula("2023001");
        request.setSenha("Senha1234");
        request.setEmail("maria@teste.com");
        request.setTelefone("83999998888");
        request.setRole("ALUNO");
        return request;
    }

    // ---------- login ----------

    @Test
    void loginAutenticaComMatriculaESenhaEEmiteOsDoisTokens() {
        Usuario usuario = new Usuario();
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(usuario);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(tokenService.generateAccessToken(usuario)).thenReturn("access");
        when(tokenService.generateRefreshToken(usuario)).thenReturn("refresh");

        TokenAcessoResponse resposta = service.login(login("2023001", "Senha1234"));

        assertThat(resposta.getTokenAcesso()).isEqualTo("access");
        assertThat(resposta.getTokenRefresh()).isEqualTo("refresh");
        ArgumentCaptor<UsernamePasswordAuthenticationToken> enviado = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(enviado.capture());
        assertThat(enviado.getValue().getPrincipal()).isEqualTo("2023001");
        assertThat(enviado.getValue().getCredentials()).isEqualTo("Senha1234");
    }

    @Test
    void loginComCredenciaisInvalidasPropagaAFalhaENaoEmiteToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(login("2023001", "errada123")))
                .isInstanceOf(BadCredentialsException.class);

        verify(tokenService, never()).generateAccessToken(any());
        verify(tokenService, never()).generateRefreshToken(any());
    }

    // ---------- refresh ----------

    @Test
    void refreshValidoEmiteNovoParDeTokensDoUsuarioDoBanco() {
        Usuario usuario = new Usuario();
        usuario.setAtivo(true);
        when(tokenService.validateRefreshToken("refresh-antigo")).thenReturn("2023001");
        when(usuarioRepository.findByMatricula("2023001")).thenReturn(Optional.of(usuario));
        when(tokenService.generateAccessToken(usuario)).thenReturn("access-novo");
        when(tokenService.generateRefreshToken(usuario)).thenReturn("refresh-novo");

        TokenAcessoResponse resposta = service.refresh("refresh-antigo");

        assertThat(resposta.getTokenAcesso()).isEqualTo("access-novo");
        assertThat(resposta.getTokenRefresh()).isEqualTo("refresh-novo");
    }

    @Test
    void refreshInvalidoPropagaAFalhaDeVerificacaoSemTocarNoBanco() {
        when(tokenService.validateRefreshToken("lixo")).thenThrow(new JWTVerificationException("invalido"));

        assertThatThrownBy(() -> service.refresh("lixo")).isInstanceOf(JWTVerificationException.class);

        verify(usuarioRepository, never()).findByMatricula(any());
    }

    @Test
    void refreshDeUsuarioQueNaoExisteMaisFalha() {
        when(tokenService.validateRefreshToken("refresh")).thenReturn("2023001");
        when(usuarioRepository.findByMatricula("2023001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("refresh"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário do refresh token não existe mais.");
    }

    @Test
    void refreshDeContaDesativadaFalhaComoSeNaoExistisseENaoEmiteTokens() {
        Usuario desativado = new Usuario();
        desativado.setAtivo(false);
        when(tokenService.validateRefreshToken("refresh")).thenReturn("2023001");
        when(usuarioRepository.findByMatricula("2023001")).thenReturn(Optional.of(desativado));

        assertThatThrownBy(() -> service.refresh("refresh"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário do refresh token não existe mais.");

        verify(tokenService, never()).generateAccessToken(any());
        verify(tokenService, never()).generateRefreshToken(any());
    }

    // ---------- cadastro ----------

    @Test
    void cadastroRejeitaMatriculaJaExistente() {
        when(usuarioRepository.existsByMatricula("2023001")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastro(cadastro()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um usuário com essa matrícula.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastroRejeitaEmailJaExistente() {
        when(usuarioRepository.existsByMatricula("2023001")).thenReturn(false);
        when(usuarioRepository.existsByEmail("maria@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastro(cadastro()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe um usuário com esse e-mail.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void cadastroSalvaOUsuarioComASenhaJaHasheadaENuncaEmTextoPuro() {
        CadastrarUsuarioRequest request = cadastro();
        Usuario mapeado = new Usuario();
        when(usuarioRepository.existsByMatricula("2023001")).thenReturn(false);
        when(usuarioRepository.existsByEmail("maria@teste.com")).thenReturn(false);
        when(passwordEncoder.encode("Senha1234")).thenReturn("$2a$10$hash");
        when(usuarioMapper.cadastroRequestParaUsuario(request)).thenReturn(mapeado);

        service.cadastro(request);

        assertThat(request.getSenha()).isEqualTo("$2a$10$hash");
        verify(usuarioMapper).cadastroRequestParaUsuario(request);
        verify(usuarioRepository).save(mapeado);
    }
}
