package com.digital.biblioteca.model;

import com.digital.biblioteca.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@Entity
public class Usuario extends EntidadeBase implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String matricula;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        if (role == Role.BIBLIOTECARIO) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_BIBLIOTECARIO"),
                    new SimpleGrantedAuthority("ROLE_PROFESSOR"),
                    new SimpleGrantedAuthority("ROLE_ALUNO")
            );
        }

        if (role == Role.PROFESSOR) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_PROFESSOR"),
                    new SimpleGrantedAuthority("ROLE_ALUNO")
            );
        }

        return List.of(new SimpleGrantedAuthority("ROLE_ALUNO"));
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.matricula;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isAtivo();
    }

}
