package com.digital.biblioteca.mapper;

import com.digital.biblioteca.document.LivroDocument;
import com.digital.biblioteca.document.LivroPublicoDocument;
import com.digital.biblioteca.document.MaterialEstudoDocument;
import com.digital.biblioteca.document.TccDocument;
import com.digital.biblioteca.model.Curso;
import com.digital.biblioteca.model.Livro;
import com.digital.biblioteca.model.LivroPublico;
import com.digital.biblioteca.model.MaterialEstudo;
import com.digital.biblioteca.model.Tcc;
import com.digital.biblioteca.model.Tema;
import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.AtualizarLivroPublicoRequest;
import com.digital.biblioteca.request.AtualizarLivroRequest;
import com.digital.biblioteca.request.AtualizarMaterialEstudoRequest;
import com.digital.biblioteca.request.AtualizarTccRequest;
import com.digital.biblioteca.request.CadastrarLivroPublicoRequest;
import com.digital.biblioteca.request.CadastrarLivroRequest;
import com.digital.biblioteca.request.CadastrarMaterialEstudoRequest;
import com.digital.biblioteca.request.CadastrarTccRequest;
import com.digital.biblioteca.response.LivroPublicoResponse;
import com.digital.biblioteca.response.LivroResponse;
import com.digital.biblioteca.response.MaterialEstudoResponse;
import com.digital.biblioteca.response.TccResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
        ComumMapperImpl.class, CursoMapperImpl.class, TemaMapperImpl.class,
        LivroMapperImpl.class, LivroPublicoMapperImpl.class, MaterialEstudoMapperImpl.class, TccMapperImpl.class
})
class MappersDeMaterialTest {

    static final LocalDateTime DATA = LocalDateTime.of(2025, 5, 20, 10, 0);

    @Autowired LivroMapper livroMapper;
    @Autowired LivroPublicoMapper livroPublicoMapper;
    @Autowired MaterialEstudoMapper materialEstudoMapper;
    @Autowired TccMapper tccMapper;

    // ---------- Livro ----------

    @Test
    void cadastroDeLivroCopiaEscalaresEDeixaRelacoesECapaParaOServico() {
        CadastrarLivroRequest request = new CadastrarLivroRequest();
        request.setTitulo("Clean Code");
        request.setAutor("Robert Martin");
        request.setDescricao("Boas praticas");
        request.setTemas(List.of(UUID.randomUUID()));
        request.setCursos(List.of(UUID.randomUUID()));
        request.setDataPublicacao(DATA);
        request.setQuantidade(5);

        Livro livro = livroMapper.cadastroRequestParaLivro(request);

        assertThat(livro.getTitulo()).isEqualTo("Clean Code");
        assertThat(livro.getAutor()).isEqualTo("Robert Martin");
        assertThat(livro.getDescricao()).isEqualTo("Boas praticas");
        assertThat(livro.getDataPublicacao()).isEqualTo(DATA);
        assertThat(livro.getQuantidade()).isEqualTo(5);
        assertThat(livro.getId()).isNull();
        assertThat(livro.getCapa()).isNull();
        assertThat(livro.getTemas()).isNull();
        assertThat(livro.getCursos()).isNull();
        assertThat(livro.isAtivo()).isFalse();
    }

    @Test
    void livroViraDocumentComCursosETemasAninhados() {
        Livro livro = livroCompleto();

        LivroDocument document = livroMapper.livroParaDocument(livro);

        assertThat(document.getId()).isEqualTo(livro.getId().toString());
        assertThat(document.getTitulo()).isEqualTo("Clean Code");
        assertThat(document.getQuantidade()).isEqualTo(3);
        assertThat(document.getDataPublicacao()).isEqualTo(DATA);
        assertThat(document.getCursos()).extracting("nome").containsExactly("Computação");
        assertThat(document.getCursos().get(0).getId()).isEqualTo(livro.getCursos().get(0).getId().toString());
        assertThat(document.getTemas()).extracting("nome").containsExactly("Algoritmos");
    }

    @Test
    void livroViraResponseComCursosETemasAninhados() {
        Livro livro = livroCompleto();

        LivroResponse response = livroMapper.livroParaResponse(livro);

        assertThat(response.getId()).isEqualTo(livro.getId());
        assertThat(response.getQuantidade()).isEqualTo(3);
        assertThat(response.getCursos()).extracting("nome").containsExactly("Computação");
        assertThat(response.getTemas()).extracting("nome").containsExactly("Algoritmos");
    }

    @Test
    void livroSemCursosETemasGeraListasNulasSemQuebrar() {
        Livro livro = new Livro();
        livro.setId(UUID.randomUUID());

        LivroResponse response = livroMapper.livroParaResponse(livro);

        assertThat(response.getCursos()).isNull();
        assertThat(response.getTemas()).isNull();
    }

    @Test
    void atualizacaoDeLivroSobrescreveEscalaresEPreservaIdCapaERelacoes() {
        Livro existente = livroCompleto();
        UUID idOriginal = existente.getId();
        List<Curso> cursosOriginais = existente.getCursos();
        existente.setCapa("capa-antiga.png");
        AtualizarLivroRequest request = new AtualizarLivroRequest();
        request.setId(UUID.randomUUID()); // id do request diferente: nunca deve vazar pro registro
        request.setTitulo("Clean Code 2a ed.");
        request.setAutor("Robert C. Martin");
        request.setDescricao("Revisado");
        request.setCursos(List.of(UUID.randomUUID()));
        request.setTemas(List.of(UUID.randomUUID()));
        request.setDataPublicacao(DATA.plusYears(1));
        request.setQuantidade(9);

        livroMapper.atualizarRequestParaLivro(request, existente);

        assertThat(existente.getTitulo()).isEqualTo("Clean Code 2a ed.");
        assertThat(existente.getAutor()).isEqualTo("Robert C. Martin");
        assertThat(existente.getDescricao()).isEqualTo("Revisado");
        assertThat(existente.getQuantidade()).isEqualTo(9);
        assertThat(existente.getDataPublicacao()).isEqualTo(DATA.plusYears(1));
        assertThat(existente.getId()).isEqualTo(idOriginal);
        assertThat(existente.getCapa()).isEqualTo("capa-antiga.png");
        assertThat(existente.getCursos()).isSameAs(cursosOriginais);
    }

    // ---------- LivroPublico ----------

    @Test
    void cadastroDeLivroPublicoCopiaEscalaresEIgnoraCapaEConteudo() {
        CadastrarLivroPublicoRequest request = new CadastrarLivroPublicoRequest();
        request.setTitulo("Dom Casmurro");
        request.setAutor("Machado de Assis");
        request.setDescricao("Romance");
        request.setCursos(List.of(UUID.randomUUID()));
        request.setTemas(List.of(UUID.randomUUID()));
        request.setDataPublicacao(DATA);

        LivroPublico livro = livroPublicoMapper.cadastroRequestParaLivroPublico(request);

        assertThat(livro.getTitulo()).isEqualTo("Dom Casmurro");
        assertThat(livro.getDataPublicacao()).isEqualTo(DATA);
        assertThat(livro.getId()).isNull();
        assertThat(livro.getCapa()).isNull();
        assertThat(livro.getConteudo()).isNull();
    }

    @Test
    void livroPublicoViraDocumentEResponseIncluindoCapaEConteudoNoDocument() {
        LivroPublico livro = new LivroPublico();
        livro.setId(UUID.randomUUID());
        livro.setTitulo("Dom Casmurro");
        livro.setCapa("capa.png");
        livro.setConteudo("dom.pdf");
        livro.setCursos(List.of(curso("Letras")));
        livro.setTemas(List.of(tema("Romance")));

        LivroPublicoDocument document = livroPublicoMapper.livroPublicoParaDocument(livro);
        LivroPublicoResponse response = livroPublicoMapper.livroPublicoParaResponse(livro);

        assertThat(document.getCapa()).isEqualTo("capa.png");
        assertThat(document.getConteudo()).isEqualTo("dom.pdf");
        assertThat(document.getCursos()).extracting("nome").containsExactly("Letras");
        assertThat(response.getId()).isEqualTo(livro.getId());
        assertThat(response.getTemas()).extracting("nome").containsExactly("Romance");
    }

    @Test
    void atualizacaoDeLivroPublicoPreservaCapaEConteudoERelacoes() {
        LivroPublico existente = new LivroPublico();
        UUID id = UUID.randomUUID();
        existente.setId(id);
        existente.setCapa("capa.png");
        existente.setConteudo("dom.pdf");
        List<Curso> cursos = List.of(curso("Letras"));
        existente.setCursos(cursos);
        AtualizarLivroPublicoRequest request = new AtualizarLivroPublicoRequest();
        request.setId(id);
        request.setTitulo("Novo titulo");
        request.setAutor("Autor");
        request.setDescricao("Desc");
        request.setCursos(List.of(UUID.randomUUID()));
        request.setTemas(List.of(UUID.randomUUID()));
        request.setDataPublicacao(DATA);

        livroPublicoMapper.atualizarRequestParaLivroPublico(request, existente);

        assertThat(existente.getTitulo()).isEqualTo("Novo titulo");
        assertThat(existente.getId()).isEqualTo(id);
        assertThat(existente.getCapa()).isEqualTo("capa.png");
        assertThat(existente.getConteudo()).isEqualTo("dom.pdf");
        assertThat(existente.getCursos()).isSameAs(cursos);
    }

    // ---------- MaterialEstudo ----------

    @Test
    void cadastroDeMaterialDeEstudoIgnoraConteudoECriador() {
        CadastrarMaterialEstudoRequest request = new CadastrarMaterialEstudoRequest();
        request.setTitulo("Lista de exercicios");
        request.setAutor("Prof. Ana");
        request.setDescricao("Cap. 1");
        request.setTemas(List.of(UUID.randomUUID()));
        request.setCursos(List.of(UUID.randomUUID()));
        request.setDataPublicacao(DATA);

        MaterialEstudo material = materialEstudoMapper.cadastroRequestParaMaterialEstudo(request);

        assertThat(material.getTitulo()).isEqualTo("Lista de exercicios");
        assertThat(material.getConteudo()).isNull();
        assertThat(material.getCriador()).isNull();
        assertThat(material.getId()).isNull();
    }

    @Test
    void materialDeEstudoViraDocumentEResponse() {
        MaterialEstudo material = new MaterialEstudo();
        material.setId(UUID.randomUUID());
        material.setTitulo("Lista");
        material.setDataPublicacao(DATA);
        material.setCursos(List.of(curso("Computação")));
        material.setTemas(List.of(tema("Algoritmos")));

        MaterialEstudoDocument document = materialEstudoMapper.materialEstudoParaDocument(material);
        MaterialEstudoResponse response = materialEstudoMapper.materialEstudoParaResponse(material);

        assertThat(document.getId()).isEqualTo(material.getId().toString());
        assertThat(document.getDataPublicacao()).isEqualTo(DATA);
        assertThat(document.getCursos()).extracting("nome").containsExactly("Computação");
        assertThat(response.getTemas()).extracting("nome").containsExactly("Algoritmos");
    }

    @Test
    void atualizacaoDeMaterialDeEstudoPreservaConteudoECriador() {
        MaterialEstudo existente = new MaterialEstudo();
        UUID id = UUID.randomUUID();
        existente.setId(id);
        existente.setConteudo("lista.pdf");
        Usuario criador = new Usuario();
        criador.setMatricula("2023001");
        existente.setCriador(criador);
        AtualizarMaterialEstudoRequest request = new AtualizarMaterialEstudoRequest();
        request.setId(id);
        request.setTitulo("Titulo novo");
        request.setAutor("Autor");
        request.setDescricao("Desc");
        request.setTemas(List.of(UUID.randomUUID()));
        request.setCursos(List.of(UUID.randomUUID()));
        request.setDataPublicacao(DATA);

        materialEstudoMapper.atualizarRequestParaMaterialEstudo(request, existente);

        assertThat(existente.getTitulo()).isEqualTo("Titulo novo");
        assertThat(existente.getConteudo()).isEqualTo("lista.pdf");
        assertThat(existente.getCriador()).isSameAs(criador);
    }

    // ---------- Tcc ----------

    @Test
    void cadastroDeTccIgnoraCursoTemasEConteudoQueSaoResolvidosNoServico() {
        CadastrarTccRequest request = new CadastrarTccRequest();
        request.setTitulo("Sistema de biblioteca");
        request.setAutor("Joao");
        request.setDescricao("TCC");
        request.setTemas(List.of(UUID.randomUUID()));
        request.setCurso(UUID.randomUUID());
        request.setDataPublicacao(DATA);

        Tcc tcc = tccMapper.cadastroRequestParaTcc(request);

        assertThat(tcc.getTitulo()).isEqualTo("Sistema de biblioteca");
        assertThat(tcc.getDataPublicacao()).isEqualTo(DATA);
        assertThat(tcc.getCurso()).isNull();
        assertThat(tcc.getTemas()).isNull();
        assertThat(tcc.getConteudo()).isNull();
        assertThat(tcc.getId()).isNull();
    }

    @Test
    void tccViraDocumentEResponseComCursoUnico() {
        Tcc tcc = new Tcc();
        tcc.setId(UUID.randomUUID());
        tcc.setTitulo("Sistema de biblioteca");
        tcc.setCurso(curso("Computação"));
        tcc.setTemas(List.of(tema("Software")));
        tcc.setDataPublicacao(DATA);

        TccDocument document = tccMapper.tccParaDocument(tcc);
        TccResponse response = tccMapper.tccParaResponse(tcc);

        assertThat(document.getId()).isEqualTo(tcc.getId().toString());
        assertThat(document.getCurso().getNome()).isEqualTo("Computação");
        assertThat(response.getCurso().getNome()).isEqualTo("Computação");
        assertThat(response.getTemas()).extracting("nome").containsExactly("Software");
        assertThat(response.getDataPublicacao()).isEqualTo(DATA);
    }

    @Test
    void atualizacaoDeTccPreservaConteudoCursoETemas() {
        Tcc existente = new Tcc();
        UUID id = UUID.randomUUID();
        existente.setId(id);
        existente.setConteudo("tcc.pdf");
        Curso curso = curso("Computação");
        existente.setCurso(curso);
        AtualizarTccRequest request = new AtualizarTccRequest();
        request.setId(id);
        request.setTitulo("Titulo novo");
        request.setAutor("Autor");
        request.setDescricao("Desc");
        request.setTemas(List.of(UUID.randomUUID()));
        request.setCurso(UUID.randomUUID());
        request.setDataPublicacao(DATA);

        tccMapper.atualizarRequestParaTcc(request, existente);

        assertThat(existente.getTitulo()).isEqualTo("Titulo novo");
        assertThat(existente.getId()).isEqualTo(id);
        assertThat(existente.getConteudo()).isEqualTo("tcc.pdf");
        assertThat(existente.getCurso()).isSameAs(curso);
    }

    // ---------- helpers ----------

    private static Livro livroCompleto() {
        Livro livro = new Livro();
        livro.setId(UUID.randomUUID());
        livro.setTitulo("Clean Code");
        livro.setAutor("Robert Martin");
        livro.setDescricao("Boas praticas");
        livro.setQuantidade(3);
        livro.setDataPublicacao(DATA);
        livro.setCursos(List.of(curso("Computação")));
        livro.setTemas(List.of(tema("Algoritmos")));
        return livro;
    }

    private static Curso curso(String nome) {
        Curso curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setNome(nome);
        return curso;
    }

    private static Tema tema(String nome) {
        Tema tema = new Tema();
        tema.setId(UUID.randomUUID());
        tema.setNome(nome);
        return tema;
    }
}
