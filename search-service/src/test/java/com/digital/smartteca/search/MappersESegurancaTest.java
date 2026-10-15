package com.digital.smartteca.search;

import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.document.TemaDocument;
import com.digital.smartteca.search.mapper.ComumMapper;
import com.digital.smartteca.search.mapper.ComumMapperImpl;
import com.digital.smartteca.search.mapper.CursoMapperImpl;
import com.digital.smartteca.search.mapper.LivroMapper;
import com.digital.smartteca.search.mapper.LivroMapperImpl;
import com.digital.smartteca.search.mapper.ReservaMapper;
import com.digital.smartteca.search.mapper.ReservaMapperImpl;
import com.digital.smartteca.search.mapper.TccMapper;
import com.digital.smartteca.search.mapper.TccMapperImpl;
import com.digital.smartteca.search.mapper.TemaMapperImpl;
import com.digital.smartteca.search.response.LivroResponse;
import com.digital.smartteca.search.response.ReservaResponse;
import com.digital.smartteca.search.response.TccResponse;
import com.digital.smartteca.search.security.CorrelationIdFilter;
import com.digital.smartteca.search.security.GatewayHeaderAuthFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(classes = {ComumMapperImpl.class, CursoMapperImpl.class, TemaMapperImpl.class,
        LivroMapperImpl.class, TccMapperImpl.class, ReservaMapperImpl.class})
class MappersESegurancaTest {

    @Autowired ComumMapper comum;
    @Autowired LivroMapper livroMapper;
    @Autowired TccMapper tccMapper;
    @Autowired ReservaMapper reservaMapper;

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    // ---------- mappers ----------

    @Test
    void idsStringDoElasticsearchViramUuidNaResposta() {
        UUID id = UUID.randomUUID();
        CursoDocument curso = new CursoDocument();
        curso.setId(UUID.randomUUID().toString());
        curso.setNome("Direito");
        TemaDocument tema = new TemaDocument();
        tema.setId(UUID.randomUUID().toString());
        tema.setNome("Penal");
        LivroDocument documento = new LivroDocument();
        documento.setId(id.toString());
        documento.setTitulo("Clean Code");
        documento.setQuantidade(4);
        documento.setDataPublicacao(LocalDateTime.of(2020, 5, 1, 10, 0));
        documento.setCursos(List.of(curso));
        documento.setTemas(List.of(tema));

        LivroResponse resposta = livroMapper.documentParaResponse(documento);

        assertThat(resposta.getId()).isEqualTo(id);
        assertThat(resposta.getTitulo()).isEqualTo("Clean Code");
        assertThat(resposta.getQuantidade()).isEqualTo(4);
        assertThat(resposta.getCursos()).extracting("nome").containsExactly("Direito");
        assertThat(resposta.getCursos().get(0).getId().toString()).isEqualTo(curso.getId());
        assertThat(resposta.getTemas()).extracting("nome").containsExactly("Penal");
    }

    @Test
    void listaDeDocumentosViraListaDeRespostasNaMesmaOrdem() {
        LivroDocument a = new LivroDocument();
        a.setId(UUID.randomUUID().toString());
        a.setTitulo("A");
        LivroDocument b = new LivroDocument();
        b.setId(UUID.randomUUID().toString());
        b.setTitulo("B");

        assertThat(livroMapper.listaDocumentParaResponse(List.of(a, b))).extracting(LivroResponse::getTitulo).containsExactly("A", "B");
        assertThat(livroMapper.listaDocumentParaResponse(List.of())).isEmpty();
    }

    @Test
    void tccMantemOCursoUnicoNaResposta() {
        CursoDocument curso = new CursoDocument();
        curso.setId(UUID.randomUUID().toString());
        curso.setNome("Computação");
        TccDocument tcc = new TccDocument();
        tcc.setId(UUID.randomUUID().toString());
        tcc.setCurso(curso);

        TccResponse resposta = tccMapper.documentParaResponse(tcc);

        assertThat(resposta.getCurso().getNome()).isEqualTo("Computação");
    }

    @Test
    void reservaMantemStatusEDatas() {
        ReservaDocument documento = new ReservaDocument();
        documento.setId(UUID.randomUUID().toString());
        documento.setLivroId(UUID.randomUUID().toString());
        documento.setMatriculaUsuario("2023001");
        documento.setStatusReserva(StatusReserva.ATRASADO);
        documento.setDataDevolucao(LocalDateTime.of(2026, 1, 8, 9, 0));

        ReservaResponse resposta = reservaMapper.documentParaResponse(documento);

        assertThat(resposta.getStatusReserva()).isEqualTo(StatusReserva.ATRASADO);
        assertThat(resposta.getMatriculaUsuario()).isEqualTo("2023001");
        assertThat(resposta.getDataDevolucao()).isEqualTo(LocalDateTime.of(2026, 1, 8, 9, 0));
    }

    @Test
    void conversaoDeIdNuloDevolveNuloEIdInvalidoFalha() {
        assertThat(comum.map((String) null)).isNull();
        assertThat(comum.map((UUID) null)).isNull();
        assertThatThrownBy(() -> comum.map("nao-e-uuid")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void documentoNuloViraRespostaNula() {
        assertThat(livroMapper.documentParaResponse(null)).isNull();
    }

    // ---------- filtros de seguranca ----------

    private Authentication autenticacaoAposFiltrar(String matricula, String roles) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (matricula != null) {
            request.addHeader("X-User-Matricula", matricula);
        }
        if (roles != null) {
            request.addHeader("X-User-Roles", roles);
        }
        AtomicReference<Authentication> naCadeia = new AtomicReference<>();
        FilterChain chain = (req, res) -> naCadeia.set(SecurityContextHolder.getContext().getAuthentication());
        new GatewayHeaderAuthFilter().doFilter(request, new MockHttpServletResponse(), chain);
        SecurityContextHolder.clearContext();
        return naCadeia.get();
    }

    @Test
    void headersDoGatewayViramAutenticacaoComPapeisPrefixadosComROLE() throws Exception {
        Authentication auth = autenticacaoAposFiltrar("2023001", "BIBLIOTECARIO,PROFESSOR, ALUNO");

        assertThat(auth.getName()).isEqualTo("2023001");
        assertThat(auth.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_BIBLIOTECARIO", "ROLE_PROFESSOR", "ROLE_ALUNO");
    }

    @Test
    void semHeadersCompletosNaoAutentica() throws Exception {
        assertThat(autenticacaoAposFiltrar(null, null)).isNull();
        assertThat(autenticacaoAposFiltrar("2023001", null)).isNull();
        assertThat(autenticacaoAposFiltrar(null, "ALUNO")).isNull();
        assertThat(autenticacaoAposFiltrar(" ", "ALUNO")).isNull();
        assertThat(autenticacaoAposFiltrar("2023001", " ")).isNull();
    }

    @Test
    void correlationIdVaiParaMdcERespostaESaiDepoisMesmoComExcecao() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationId.HEADER, "corr-search");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdc = new AtomicReference<>();

        assertThatThrownBy(() -> new CorrelationIdFilter().doFilter(request, response, (req, res) -> {
            mdc.set(MDC.get(CorrelationId.MDC_KEY));
            throw new IllegalStateException("falha");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(mdc.get()).isEqualTo("corr-search");
        assertThat(response.getHeader(CorrelationId.HEADER)).isEqualTo("corr-search");
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }
}
