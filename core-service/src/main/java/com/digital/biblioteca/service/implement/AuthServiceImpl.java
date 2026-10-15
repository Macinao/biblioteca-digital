package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.config.security.TokenService;
import com.digital.biblioteca.mapper.UsuarioMapper;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.UsuarioRepository;
import com.digital.biblioteca.request.CadastrarUsuarioRequest;
import com.digital.biblioteca.request.LoginUsuarioRequest;
import com.digital.biblioteca.response.TokenAcessoResponse;
import com.digital.biblioteca.service.AuthService;
import com.digital.biblioteca.treatment.RecursoNaoEncontradoException;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public TokenAcessoResponse login(LoginUsuarioRequest request) {

        var userNamePassword = new UsernamePasswordAuthenticationToken(request.getMatricula(), request.getSenha());
        var auth = this.authenticationManager.authenticate(userNamePassword);

        return new TokenAcessoResponse(
                tokenService.generateAccessToken((Usuario) auth.getPrincipal()),
                tokenService.generateRefreshToken((Usuario) auth.getPrincipal()));

    }

    @Override
    public TokenAcessoResponse refresh(String refreshToken) {

        String matricula = tokenService.validateRefreshToken(refreshToken);

        // Conta desativada conta como inexistente: sem isso o refresh token renovaria o acesso para sempre.
        Usuario usuario = usuarioRepository.findByMatricula(matricula)
                .filter(Usuario::isEnabled)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário do refresh token não existe mais."));

        return new TokenAcessoResponse(
                tokenService.generateAccessToken(usuario),
                tokenService.generateRefreshToken(usuario));

    }

    @Override
    public void cadastro(CadastrarUsuarioRequest request) {

        if (usuarioRepository.existsByMatricula(request.getMatricula())) {
            throw new RegraNegocioException("Já existe um usuário com essa matrícula.");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RegraNegocioException("Já existe um usuário com esse e-mail.");
        }

        request.setSenha(passwordEncoder.encode(request.getSenha()));
        usuarioRepository.save(usuarioMapper.cadastroRequestParaUsuario(request));

    }
}
