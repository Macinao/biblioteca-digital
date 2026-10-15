package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.CursoDocument;
import com.digital.biblioteca.document.ReservaDocument;
import com.digital.biblioteca.document.TemaDocument;
import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Reserva;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.CadastrarCursoRequest;
import com.digital.biblioteca.request.CadastrarTemaRequest;
import com.digital.biblioteca.request.CadastrarUsuarioRequest;
import com.digital.biblioteca.response.CursoResponse;
import com.digital.biblioteca.response.TemaResponse;
import com.digital.smartteca.common.enums.StatusReserva;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = {
        ComumMapperImpl.class, CursoMapperImpl.class, TemaMapperImpl.class,
        ReservaMapperImpl.class, UsuarioMapperImpl.class
})
class MappersDeCatalogoTest {

    @Autowired ComumMapper comum;
    @Autowired CursoMapper cursoMapper;
    @Autowired TemaMapper temaMapper;
    @Autowired ReservaMapper reservaMapper;
    @Autowired UsuarioMapper usuarioMapper;

    // ---------- ComumMapper ----------

    @Test
    void uuidViraStringEVoltaIgual() {
        UUID id = UUID.randomUUID();

        assertThat(comum.map(id)).isEqualTo(id.toString());
        assertThat(comum.map(id.toString())).isEqualTo(id);
    }

    @Test
    void conversaoDeNuloDevolveNulo() {
        assertThat(comum.map((UUID) null)).isNull();
        assertThat(comum.map((String) null)).isNull();
    }

    @Test
    void stringQueNaoEUuidFalha() {
        assertThatThrownBy(() -> comum.map("nao-e-uuid")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- CursoMapper ----------

    @Test
    void cadastroDeCursoCopiaONomeEDeixaOsCamposGeridosPeloBancoVazios() {
        CadastrarCursoRequest request = new CadastrarCursoRequest();
        request.setNome("Ciência da Computação");

        Curso curso = cursoMapper.cadastroRequestParaCurso(request);

        assertThat(curso.getNome()).isEqualTo("Ciência da Computação");
        assertThat(curso.getId()).isNull();
        assertThat(curso.getDataCriacao()).isNull();
        assertThat(curso.isAtivo()).isFalse();
    }

    @Test
    void cursoViraDocumentComIdEmString() {
        Curso curso = curso("Direito");

        CursoDocument document = cursoMapper.cursoParaDocument(curso);

        assertThat(document.getId()).isEqualTo(curso.getId().toString());
        assertThat(document.getNome()).isEqualTo("Direito");
    }

    @Test
    void cursoViraResponse() {
        Curso curso = curso("Direito");

        CursoResponse response = cursoMapper.cursoParaResponse(curso);

        assertThat(response.getId()).isEqualTo(curso.getId());
        assertThat(response.getNome()).isEqualTo("Direito");
    }

    @Test
    void entradaNulaDevolveNuloEmTodosOsMapeamentosDeCurso() {
        assertThat(cursoMapper.cadastroRequestParaCurso(null)).isNull();
        assertThat(cursoMapper.cursoParaDocument(null)).isNull();
        assertThat(cursoMapper.cursoParaResponse(null)).isNull();
    }

    // ---------- TemaMapper ----------

    @Test
    void cadastroDeTemaCopiaONomeEIgnoraCamposGeridos() {
        CadastrarTemaRequest request = new CadastrarTemaRequest();
        request.setNome("Algoritmos");

        Tema tema = temaMapper.cadastroRequestParaTema(request);

        assertThat(tema.getNome()).isEqualTo("Algoritmos");
        assertThat(tema.getId()).isNull();
        assertThat(tema.isAtivo()).isFalse();
    }

    @Test
    void temaViraDocumentEResponse() {
        Tema tema = new Tema();
        tema.setId(UUID.randomUUID());
        tema.setNome("Algoritmos");

        TemaDocument document = temaMapper.temaParaDocument(tema);
        TemaResponse response = temaMapper.temaParaResponse(tema);

        assertThat(document.getId()).isEqualTo(tema.getId().toString());
        assertThat(document.getNome()).isEqualTo("Algoritmos");
        assertThat(response.getId()).isEqualTo(tema.getId());
        assertThat(response.getNome()).isEqualTo("Algoritmos");
    }

    // ---------- UsuarioMapper ----------

    @Test
    void cadastroDeUsuarioConverteARoleDeStringParaEnumEMantemAsDemaisInformacoes() {
        CadastrarUsuarioRequest request = requestUsuario("PROFESSOR");

        Usuario usuario = usuarioMapper.cadastroRequestParaUsuario(request);

        assertThat(usuario.getNome()).isEqualTo("Maria Souza");
        assertThat(usuario.getMatricula()).isEqualTo("2023001");
        assertThat(usuario.getEmail()).isEqualTo("maria@teste.com");
        assertThat(usuario.getTelefone()).isEqualTo("83999998888");
        assertThat(usuario.getRole()).isEqualTo(Role.PROFESSOR);
        assertThat(usuario.getId()).isNull();
        assertThat(usuario.isAtivo()).isFalse();
    }

    @Test
    void mapperDeUsuarioNaoFazHashDaSenha() {
        // O hash e responsabilidade do AuthService/UsuarioService; o mapper so repassa o texto recebido.
        Usuario usuario = usuarioMapper.cadastroRequestParaUsuario(requestUsuario("ALUNO"));

        assertThat(usuario.getSenha()).isEqualTo("Senha1234");
    }

    @Test
    void roleForaDoEnumFalhaNaConversao() {
        assertThatThrownBy(() -> usuarioMapper.cadastroRequestParaUsuario(requestUsuario("DIRETOR")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- ReservaMapper ----------

    @Test
    void reservaViraDocumentAchatandoLivroEUsuario() {
        Livro livro = new Livro();
        livro.setId(UUID.randomUUID());
        livro.setTitulo("Clean Code");
        livro.setAutor("Robert Martin");
        Usuario usuario = new Usuario();
        usuario.setMatricula("2023001");
        usuario.setNome("Maria Souza");
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());
        reserva.setLivro(livro);
        reserva.setUsuario(usuario);
        reserva.setStatusReserva(StatusReserva.ATRASADO);
        reserva.setDataCriacao(LocalDateTime.of(2026, 1, 10, 9, 0));
        reserva.setDataDevolucao(LocalDateTime.of(2026, 1, 17, 9, 0));

        ReservaDocument document = reservaMapper.reservaParaDocument(reserva);

        assertThat(document.getId()).isEqualTo(reserva.getId().toString());
        assertThat(document.getLivroId()).isEqualTo(livro.getId().toString());
        assertThat(document.getTituloLivro()).isEqualTo("Clean Code");
        assertThat(document.getAutorLivro()).isEqualTo("Robert Martin");
        assertThat(document.getMatriculaUsuario()).isEqualTo("2023001");
        assertThat(document.getNomeUsuario()).isEqualTo("Maria Souza");
        assertThat(document.getStatusReserva()).isEqualTo(StatusReserva.ATRASADO);
        assertThat(document.getDataCriacao()).isEqualTo(LocalDateTime.of(2026, 1, 10, 9, 0));
        assertThat(document.getDataDevolucao()).isEqualTo(LocalDateTime.of(2026, 1, 17, 9, 0));
    }

    @Test
    void reservaSemLivroEUsuarioGeraDocumentComCamposAchatadosNulos() {
        Reserva reserva = new Reserva();
        reserva.setId(UUID.randomUUID());

        ReservaDocument document = reservaMapper.reservaParaDocument(reserva);

        assertThat(document.getLivroId()).isNull();
        assertThat(document.getTituloLivro()).isNull();
        assertThat(document.getMatriculaUsuario()).isNull();
        assertThat(document.getNomeUsuario()).isNull();
    }

    private static Curso curso(String nome) {
        Curso curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setNome(nome);
        return curso;
    }

    private static CadastrarUsuarioRequest requestUsuario(String role) {
        CadastrarUsuarioRequest request = new CadastrarUsuarioRequest();
        request.setNome("Maria Souza");
        request.setMatricula("2023001");
        request.setSenha("Senha1234");
        request.setEmail("maria@teste.com");
        request.setTelefone("83999998888");
        request.setRole(role);
        return request;
    }
}
