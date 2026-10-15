package com.digital.biblioteca.model;

import com.digital.biblioteca.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioETDominioTest {

    private static Usuario com(Role role) {
        Usuario usuario = new Usuario();
        usuario.setRole(role);
        return usuario;
    }

    @Test
    void bibliotecarioTemOsTresPapeisEmHierarquia() {
        assertThat(com(Role.BIBLIOTECARIO).getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_BIBLIOTECARIO", "ROLE_PROFESSOR", "ROLE_ALUNO");
    }

    @Test
    void professorTemProfessorEAluno() {
        assertThat(com(Role.PROFESSOR).getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_PROFESSOR", "ROLE_ALUNO");
    }

    @Test
    void alunoTemSoOPapelDeAluno() {
        assertThat(com(Role.ALUNO).getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ALUNO");
    }

    @Test
    void roleNuloCaiNoMenorPrivilegio() {
        assertThat(com(null).getAuthorities()).extracting(Object::toString)
                .containsExactly("ROLE_ALUNO");
    }

    @Test
    void usernameEAMatriculaEPasswordEASenhaHasheada() {
        Usuario usuario = new Usuario();
        usuario.setMatricula("2023001");
        usuario.setSenha("$2a$10$hash");

        assertThat(usuario.getUsername()).isEqualTo("2023001");
        assertThat(usuario.getPassword()).isEqualTo("$2a$10$hash");
    }

    @Test
    void contaInativaFicaDesabilitadaEAsDemaisFlagsContinuamTrue() {
        Usuario inativo = new Usuario();
        inativo.setAtivo(false);
        Usuario ativo = new Usuario();
        ativo.setAtivo(true);

        assertThat(inativo.isEnabled()).isFalse();
        assertThat(ativo.isEnabled()).isTrue();
        assertThat(inativo.isAccountNonExpired()).isTrue();
        assertThat(inativo.isAccountNonLocked()).isTrue();
        assertThat(inativo.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void prePersistirDefineDataDeCriacaoEAtivaOCadastro() {
        Usuario usuario = new Usuario();

        usuario.prePersistir();

        assertThat(usuario.getDataCriacao()).isNotNull();
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.getDataAtualizacao()).isNull();
    }

    @Test
    void preAtualizarDefineSoADataDeAtualizacao() {
        Usuario usuario = new Usuario();

        usuario.preAtualizar();

        assertThat(usuario.getDataAtualizacao()).isNotNull();
        assertThat(usuario.getDataCriacao()).isNull();
    }
}
