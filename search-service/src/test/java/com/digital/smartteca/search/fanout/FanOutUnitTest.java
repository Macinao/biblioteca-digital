package com.digital.smartteca.search.fanout;

import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.document.TemaDocument;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FanOutUnitTest {

    final LivroDocumentRepository livros = mock(LivroDocumentRepository.class);
    final LivroPublicoDocumentRepository livrosPublicos = mock(LivroPublicoDocumentRepository.class);
    final TccDocumentRepository tccs = mock(TccDocumentRepository.class);
    final MaterialEstudoDocumentRepository materiais = mock(MaterialEstudoDocumentRepository.class);

    static CursoDocument curso(String id, String nome) {
        CursoDocument curso = new CursoDocument();
        curso.setId(id);
        curso.setNome(nome);
        return curso;
    }

    static TemaDocument tema(String id, String nome) {
        TemaDocument tema = new TemaDocument();
        tema.setId(id);
        tema.setNome(nome);
        return tema;
    }

    // =====================  CURSO  =====================

    @Test
    void renomearCursoSubstituiACopiaEmbutidaEmLivroLivroPublicoMaterialEPreservaOsOutrosCursos() {
        CursoDocument antigo = curso("c1", "Direito");
        CursoDocument outro = curso("c2", "Medicina");
        LivroDocument livro = new LivroDocument();
        livro.setCursos(new ArrayList<>(List.of(antigo, outro)));
        LivroPublicoDocument publico = new LivroPublicoDocument();
        publico.setCursos(new ArrayList<>(List.of(antigo)));
        MaterialEstudoDocument material = new MaterialEstudoDocument();
        material.setCursos(new ArrayList<>(List.of(outro, antigo)));
        when(livros.findByCursosId("c1")).thenReturn(List.of(livro));
        when(livrosPublicos.findByCursosId("c1")).thenReturn(List.of(publico));
        when(materiais.findByCursosId("c1")).thenReturn(List.of(material));
        when(tccs.findByCursoId("c1")).thenReturn(List.of());
        CursoDocument novo = curso("c1", "Direito Penal");

        new CursoFanOutService(livros, livrosPublicos, tccs, materiais).propagarAtualizacao(novo);

        assertThat(livro.getCursos()).extracting(CursoDocument::getNome).containsExactly("Direito Penal", "Medicina");
        assertThat(publico.getCursos()).extracting(CursoDocument::getNome).containsExactly("Direito Penal");
        assertThat(material.getCursos()).extracting(CursoDocument::getNome).containsExactly("Medicina", "Direito Penal");
        verify(livros).saveAll(List.of(livro));
        verify(livrosPublicos).saveAll(List.of(publico));
        verify(materiais).saveAll(List.of(material));
    }

    @Test
    void renomearCursoReatribuiOCursoUnicoDoTcc() {
        TccDocument tcc = new TccDocument();
        tcc.setCurso(curso("c1", "Direito"));
        when(livros.findByCursosId("c1")).thenReturn(List.of());
        when(livrosPublicos.findByCursosId("c1")).thenReturn(List.of());
        when(materiais.findByCursosId("c1")).thenReturn(List.of());
        when(tccs.findByCursoId("c1")).thenReturn(List.of(tcc));
        CursoDocument novo = curso("c1", "Direito Penal");

        new CursoFanOutService(livros, livrosPublicos, tccs, materiais).propagarAtualizacao(novo);

        assertThat(tcc.getCurso()).isSameAs(novo);
        verify(tccs).saveAll(List.of(tcc));
    }

    @Test
    void semDocumentosReferenciandoOCursoSalvaListasVaziasSemErro() {
        when(livros.findByCursosId("c9")).thenReturn(List.of());
        when(livrosPublicos.findByCursosId("c9")).thenReturn(List.of());
        when(materiais.findByCursosId("c9")).thenReturn(List.of());
        when(tccs.findByCursoId("c9")).thenReturn(List.of());

        new CursoFanOutService(livros, livrosPublicos, tccs, materiais).propagarAtualizacao(curso("c9", "X"));

        verify(livros).saveAll(List.of());
        verify(tccs).saveAll(List.of());
    }

    // =====================  TEMA  =====================

    @Test
    void renomearTemaSubstituiACopiaEmbutidaEmTodosOsQuatroIndices() {
        TemaDocument antigo = tema("t1", "Algoritmos");
        TemaDocument outro = tema("t2", "Redes");
        LivroDocument livro = new LivroDocument();
        livro.setTemas(new ArrayList<>(List.of(outro, antigo)));
        LivroPublicoDocument publico = new LivroPublicoDocument();
        publico.setTemas(new ArrayList<>(List.of(antigo)));
        MaterialEstudoDocument material = new MaterialEstudoDocument();
        material.setTemas(new ArrayList<>(List.of(antigo, outro)));
        TccDocument tcc = new TccDocument();
        tcc.setTemas(new ArrayList<>(List.of(antigo)));
        when(livros.findByTemasId("t1")).thenReturn(List.of(livro));
        when(livrosPublicos.findByTemasId("t1")).thenReturn(List.of(publico));
        when(materiais.findByTemasId("t1")).thenReturn(List.of(material));
        when(tccs.findByTemasId("t1")).thenReturn(List.of(tcc));
        TemaDocument novo = tema("t1", "Estruturas de Dados");

        new TemaFanOutService(livros, livrosPublicos, tccs, materiais).propagarAtualizacao(novo);

        assertThat(livro.getTemas()).extracting(TemaDocument::getNome).containsExactly("Redes", "Estruturas de Dados");
        assertThat(publico.getTemas()).extracting(TemaDocument::getNome).containsExactly("Estruturas de Dados");
        assertThat(material.getTemas()).extracting(TemaDocument::getNome).containsExactly("Estruturas de Dados", "Redes");
        assertThat(tcc.getTemas()).extracting(TemaDocument::getNome).containsExactly("Estruturas de Dados");
        verify(livros).saveAll(List.of(livro));
        verify(livrosPublicos).saveAll(List.of(publico));
        verify(materiais).saveAll(List.of(material));
        verify(tccs).saveAll(List.of(tcc));
    }

    @Test
    void renomearTemaNaoMexeEmTemasComOutroId() {
        TemaDocument outro = tema("t2", "Redes");
        LivroDocument livro = new LivroDocument();
        livro.setTemas(new ArrayList<>(List.of(outro)));
        when(livros.findByTemasId("t1")).thenReturn(List.of(livro));
        when(livrosPublicos.findByTemasId("t1")).thenReturn(List.of());
        when(materiais.findByTemasId("t1")).thenReturn(List.of());
        when(tccs.findByTemasId("t1")).thenReturn(List.of());

        new TemaFanOutService(livros, livrosPublicos, tccs, materiais).propagarAtualizacao(tema("t1", "Novo"));

        assertThat(livro.getTemas()).containsExactly(outro);
        assertThat(livro.getTemas().get(0).getNome()).isEqualTo("Redes");
    }
}
