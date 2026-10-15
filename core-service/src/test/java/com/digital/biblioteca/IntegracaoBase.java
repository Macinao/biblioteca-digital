package com.digital.biblioteca;

import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.outbox.OutboxRelay;
import com.digital.biblioteca.repository.CursoRepository;
import com.digital.biblioteca.repository.LivroRepository;
import com.digital.biblioteca.repository.TemaRepository;
import com.digital.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Base dos testes de integracao: Spring Boot completo contra um Postgres REAL (Testcontainers),
 * com a cadeia de seguranca verdadeira. O container e compartilhado por todas as classes (sobe uma vez).
 * O OutboxRelay e substituido por mock para o agendador nao publicar/alterar as linhas de outbox
 * enquanto os testes as inspecionam; ele tem testes proprios contra Kafka e RabbitMQ reais.
 */
@SpringBootTest
public abstract class IntegracaoBase {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
    static final String CHAVE_PRIVADA;
    static final String CHAVE_PUBLICA;

    static {
        POSTGRES.start();
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair par = generator.generateKeyPair();
            CHAVE_PRIVADA = pem("PRIVATE KEY", par.getPrivate().getEncoded());
            CHAVE_PUBLICA = pem("PUBLIC KEY", par.getPublic().getEncoded());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void propriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.keys.jwt.private-key", () -> CHAVE_PRIVADA);
        registry.add("spring.keys.jwt.public-key", () -> CHAVE_PUBLICA);
    }

    @MockitoBean
    protected OutboxRelay outboxRelay;

    @Autowired protected WebApplicationContext webApplicationContext;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected CursoRepository cursoRepository;
    @Autowired protected TemaRepository temaRepository;
    @Autowired protected LivroRepository livroRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @Autowired protected PlatformTransactionManager transactionManager;

    protected MockMvc mvc;

    @BeforeEach
    void prepararAmbiente() {
        limparBanco();
        mvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    /** Roda o bloco numa transacao propria (os UPDATE @Modifying exigem uma; em producao ela vem do service). */
    protected <T> T emTransacao(Supplier<T> bloco) {
        return new TransactionTemplate(transactionManager).execute(status -> bloco.get());
    }

    protected void limparBanco() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        if (!tabelas.isEmpty()) {
            jdbc.execute("TRUNCATE TABLE " + String.join(", ", tabelas) + " RESTART IDENTITY CASCADE");
        }
    }

    // ---------- fabricas de dados persistidos ----------

    protected Usuario criarUsuario(String matricula, Role role) {
        return criarUsuario(matricula, role, "Senha1234");
    }

    protected Usuario criarUsuario(String matricula, Role role, String senhaEmTextoPuro) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario " + matricula);
        usuario.setMatricula(matricula);
        usuario.setSenha(passwordEncoder.encode(senhaEmTextoPuro));
        usuario.setEmail(matricula + "@teste.com");
        usuario.setTelefone("83999998888");
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    protected Curso criarCurso(String nome) {
        Curso curso = new Curso();
        curso.setNome(nome);
        return cursoRepository.save(curso);
    }

    protected Tema criarTema(String nome) {
        Tema tema = new Tema();
        tema.setNome(nome);
        return temaRepository.save(tema);
    }

    protected Livro criarLivro(String titulo, int quantidade) {
        Livro livro = new Livro();
        livro.setTitulo(titulo);
        livro.setAutor("Autor " + UUID.randomUUID().toString().substring(0, 4));
        livro.setDescricao("Descricao");
        livro.setQuantidade(quantidade);
        livro.setDataPublicacao(LocalDateTime.of(2020, 1, 1, 0, 0));
        livro.setCapa("capa-" + titulo);
        return livroRepository.save(livro);
    }

    private static String pem(String tipo, byte[] der) {
        return "-----BEGIN " + tipo + "-----" + Base64.getEncoder().encodeToString(der) + "-----END " + tipo + "-----";
    }
}
