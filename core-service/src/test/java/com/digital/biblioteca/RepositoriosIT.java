package com.digital.biblioteca;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.repository.ReservaRepository;
import com.digital.smartteca.common.enums.StatusReserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Repositorios e mapeamento JPA contra o Postgres real. */
class RepositoriosIT extends IntegracaoBase {

    @Autowired ReservaRepository reservaRepository;

    // ---------- usuario ----------

    @Test
    void usuarioEBuscadoPorMatriculaEExistenciaPorMatriculaOuEmail() {
        criarUsuario("2023001", Role.ALUNO);

        assertThat(usuarioRepository.findByMatricula("2023001")).isPresent();
        assertThat(usuarioRepository.findByMatricula("9999999")).isEmpty();
        assertThat(usuarioRepository.existsByMatricula("2023001")).isTrue();
        assertThat(usuarioRepository.existsByMatricula("9999999")).isFalse();
        assertThat(usuarioRepository.existsByEmail("2023001@teste.com")).isTrue();
        assertThat(usuarioRepository.existsByEmail("outro@teste.com")).isFalse();
    }

    @Test
    void bancoRejeitaMatriculaEEmailDuplicados() {
        criarUsuario("2023001", Role.ALUNO);

        Usuario duplicada = new Usuario();
        duplicada.setNome("Outro");
        duplicada.setMatricula("2023001");
        duplicada.setSenha("x");
        duplicada.setEmail("novo@teste.com");
        duplicada.setTelefone("83999998888");
        duplicada.setRole(Role.ALUNO);

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(duplicada)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void entidadePersistidaRecebeDataDeCriacaoEFicaAtiva() {
        Usuario usuario = criarUsuario("2023001", Role.ALUNO);

        Usuario lido = usuarioRepository.findById(usuario.getId()).orElseThrow();

        assertThat(lido.isAtivo()).isTrue();
        assertThat(lido.getDataCriacao()).isNotNull();
        assertThat(lido.getRole()).isEqualTo(Role.ALUNO);
    }

    // ---------- curso e tema ----------

    @Test
    void cursoBuscaPorNomeIgnorandoCaixaSoEntreOsAtivos() {
        Curso ativo = criarCurso("Direito");
        Curso inativo = criarCurso("Medicina");
        inativo.setAtivo(false);
        cursoRepository.save(inativo);

        assertThat(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrue("direito")).isTrue();
        assertThat(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrue("MEDICINA")).isFalse();
        assertThat(cursoRepository.findByIdAndAtivoTrue(ativo.getId())).isPresent();
        assertThat(cursoRepository.findByIdAndAtivoTrue(inativo.getId())).isEmpty();
    }

    @Test
    void cursoExistsComIdNotIgnoraOProprioRegistro() {
        Curso curso = criarCurso("Direito");
        Curso outro = criarCurso("Medicina");

        assertThat(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot("Direito", curso.getId())).isFalse();
        assertThat(cursoRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot("Direito", outro.getId())).isTrue();
    }

    @Test
    void temaBuscaPorNomeIgnorandoCaixaSoEntreOsAtivos() {
        Tema ativo = criarTema("Algoritmos");
        Tema inativo = criarTema("Redes");
        inativo.setAtivo(false);
        temaRepository.save(inativo);

        assertThat(temaRepository.existsByNomeIgnoreCaseAndAtivoTrue("ALGORITMOS")).isTrue();
        assertThat(temaRepository.existsByNomeIgnoreCaseAndAtivoTrue("redes")).isFalse();
        assertThat(temaRepository.findByIdAndAtivoTrue(ativo.getId())).isPresent();
        assertThat(temaRepository.findByIdAndAtivoTrue(inativo.getId())).isEmpty();
        assertThat(temaRepository.existsByNomeIgnoreCaseAndAtivoTrueAndIdNot("Algoritmos", ativo.getId())).isFalse();
    }

    // ---------- livro (heranca JOINED) ----------

    @Test
    void livroPersisteNasDuasTabelasDaHerancaEMantemAsRelacoes() {
        Curso curso = criarCurso("Computação");
        Tema tema = criarTema("Algoritmos");
        Livro livro = criarLivro("Clean Code", 3);
        livro.setCursos(List.of(curso));
        livro.setTemas(List.of(tema));
        livroRepository.save(livro);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM material_academico", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM livro", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM livro_curso", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM material_tema", Integer.class)).isEqualTo(1);
    }

    @Test
    void livroBuscaSoOsAtivos() {
        Livro ativo = criarLivro("Ativo", 1);
        Livro inativo = criarLivro("Inativo", 1);
        inativo.setAtivo(false);
        livroRepository.save(inativo);

        assertThat(livroRepository.findByIdAndAtivoTrue(ativo.getId())).isPresent();
        assertThat(livroRepository.findByIdAndAtivoTrue(inativo.getId())).isEmpty();
        assertThat(livroRepository.findByIdAndAtivoTrue(UUID.randomUUID())).isEmpty();
    }

    @Test
    void decrementarReduzUmExemplarEDevolveUmaLinhaAfetada() {
        Livro livro = criarLivro("Clean Code", 3);

        int afetadas = emTransacao(() -> livroRepository.decrementarQuantidade(livro.getId()));

        assertThat(afetadas).isEqualTo(1);
        assertThat(livroRepository.findById(livro.getId()).orElseThrow().getQuantidade()).isEqualTo(2);
    }

    @Test
    void decrementarSemEstoqueNaoAlteraENaoAfetaLinhas() {
        Livro livro = criarLivro("Esgotado", 0);

        assertThat(emTransacao(() -> livroRepository.decrementarQuantidade(livro.getId()))).isZero();
        assertThat(livroRepository.findById(livro.getId()).orElseThrow().getQuantidade()).isZero();
    }

    @Test
    void decrementarEIncrementarIgnoramLivroInativo() {
        Livro livro = criarLivro("Inativo", 5);
        livro.setAtivo(false);
        livroRepository.save(livro);

        assertThat(emTransacao(() -> livroRepository.decrementarQuantidade(livro.getId()))).isZero();
        assertThat(emTransacao(() -> livroRepository.incrementarQuantidade(livro.getId()))).isZero();
        assertThat(livroRepository.findById(livro.getId()).orElseThrow().getQuantidade()).isEqualTo(5);
    }

    @Test
    void incrementarSomaUmExemplar() {
        Livro livro = criarLivro("Clean Code", 1);

        assertThat(emTransacao(() -> livroRepository.incrementarQuantidade(livro.getId()))).isEqualTo(1);
        assertThat(livroRepository.findById(livro.getId()).orElseThrow().getQuantidade()).isEqualTo(2);
    }

    // ---------- reserva ----------

    @Test
    void reservaContaSoAsAtivasDoUsuario() {
        Usuario maria = criarUsuario("2023001", Role.ALUNO);
        Usuario joao = criarUsuario("2023002", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 10);
        salvarReserva(maria, livro, StatusReserva.RESERVADO, true, LocalDateTime.now().plusDays(7));
        salvarReserva(maria, livro, StatusReserva.RESERVADO, true, LocalDateTime.now().plusDays(7));
        salvarReserva(maria, livro, StatusReserva.RESERVADO, false, LocalDateTime.now().plusDays(7));
        salvarReserva(joao, livro, StatusReserva.RESERVADO, true, LocalDateTime.now().plusDays(7));

        assertThat(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023001")).isEqualTo(2);
        assertThat(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("2023002")).isEqualTo(1);
        assertThat(reservaRepository.countByUsuarioMatriculaAndAtivoTrue("0000000")).isZero();
    }

    @Test
    void reservaExistePorLivroSoQuandoAtiva() {
        Usuario maria = criarUsuario("2023001", Role.ALUNO);
        Livro comReserva = criarLivro("Com reserva", 5);
        Livro devolvido = criarLivro("Devolvido", 5);
        salvarReserva(maria, comReserva, StatusReserva.RESERVADO, true, LocalDateTime.now().plusDays(7));
        salvarReserva(maria, devolvido, StatusReserva.RESERVADO, false, LocalDateTime.now().plusDays(7));

        assertThat(reservaRepository.existsByLivroIdAndAtivoTrue(comReserva.getId())).isTrue();
        assertThat(reservaRepository.existsByLivroIdAndAtivoTrue(devolvido.getId())).isFalse();
    }

    @Test
    void reservasAtrasadasSaoAsAtivasEmReservadoComPrazoAntesDoCorte() {
        Usuario maria = criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 10);
        Reserva vencida = salvarReserva(maria, livro, StatusReserva.RESERVADO, true, LocalDateTime.now().minusDays(1));
        salvarReserva(maria, livro, StatusReserva.RESERVADO, true, LocalDateTime.now().plusDays(1));
        salvarReserva(maria, livro, StatusReserva.ATRASADO, true, LocalDateTime.now().minusDays(5));
        salvarReserva(maria, livro, StatusReserva.RESERVADO, false, LocalDateTime.now().minusDays(1));

        List<Reserva> resultado = reservaRepository.findByAtivoTrueAndStatusReservaAndDataDevolucaoBefore(
                StatusReserva.RESERVADO, LocalDateTime.now());

        assertThat(resultado).extracting(Reserva::getId).containsExactly(vencida.getId());
    }

    @Test
    void reservasAtivasPodemSerListadasPorStatus() {
        Usuario maria = criarUsuario("2023001", Role.ALUNO);
        Livro livro = criarLivro("Clean Code", 10);
        Reserva atrasada = salvarReserva(maria, livro, StatusReserva.ATRASADO, true, LocalDateTime.now().minusDays(5));
        salvarReserva(maria, livro, StatusReserva.RESERVADO, true, LocalDateTime.now().plusDays(1));
        salvarReserva(maria, livro, StatusReserva.ATRASADO, false, LocalDateTime.now().minusDays(5));

        assertThat(reservaRepository.findByAtivoTrueAndStatusReserva(StatusReserva.ATRASADO))
                .extracting(Reserva::getId).containsExactly(atrasada.getId());
    }

    private Reserva salvarReserva(Usuario usuario, Livro livro, StatusReserva status, boolean ativa, LocalDateTime devolucao) {
        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setLivro(livro);
        reserva.setStatusReserva(status);
        reserva.setDataDevolucao(devolucao);
        reserva = reservaRepository.save(reserva);
        if (!ativa) {
            reserva.setAtivo(false);
            reserva = reservaRepository.save(reserva);
        }
        return reserva;
    }
}
