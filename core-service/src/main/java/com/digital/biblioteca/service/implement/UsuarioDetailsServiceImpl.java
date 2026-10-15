package com.digital.biblioteca.service.implement;

import com.digital.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String matricula)
            throws UsernameNotFoundException {

        return usuarioRepository.findByMatricula(matricula)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado com matrícula: " + matricula));

    }
}
