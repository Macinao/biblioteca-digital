package com.digital.biblioteca.config;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Garante o bibliotecário inicial no boot. Como o cadastro de usuário exige um bibliotecário logado
 * (POST /api/v1/usuario), sem este seed o sistema novo não teria como ganhar o primeiro acesso.
 * Só age se as duas variáveis (ADMIN_MATRICULA e ADMIN_SENHA) vierem preenchidas e se aquela
 * matrícula ainda não existir: reiniciar o serviço nunca sobrescreve a senha de ninguém.
 */
@Slf4j
@Component
public class AdminSeeder implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String matricula;
    private final String senha;

    public AdminSeeder(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.matricula}") String matricula,
                       @Value("${app.admin.senha}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.matricula = matricula;
        this.senha = senha;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (matricula.isBlank() || senha.isBlank() || usuarioRepository.existsByMatricula(matricula)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setMatricula(matricula);
        admin.setSenha(passwordEncoder.encode(senha));
        admin.setEmail("admin@smartteca.local");
        admin.setTelefone("00000000000");
        admin.setRole(Role.BIBLIOTECARIO);
        usuarioRepository.save(admin);
        log.info("Bibliotecário inicial criado: {}", matricula);
    }
}
