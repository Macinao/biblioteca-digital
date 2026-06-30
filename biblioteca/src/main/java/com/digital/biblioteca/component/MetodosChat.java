package com.digital.biblioteca.component;

import com.digital.biblioteca.document.*;
import com.digital.biblioteca.repository.LivroDocumentRepository;
import com.digital.biblioteca.repository.LivroPublicoDocumentRepository;
import com.digital.biblioteca.repository.MaterialEstudoDocumentRepository;
import com.digital.biblioteca.repository.TccDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetodosChat {

    private final LivroDocumentRepository livroDocumentRepository;
    private final LivroPublicoDocumentRepository livroPublicoDocumentRepository;
    private final TccDocumentRepository tccDocumentRepository;
    private final MaterialEstudoDocumentRepository materialEstudoDocumentRepository;

    @Tool(description = """
        Busca livros FÍSICOS do acervo PRIVADO da biblioteca.
        Use para livros que precisam ser retirados, reservados ou emprestados.
        NÃO use para livros gratuitos ou de domínio público.
        Exemplos de uso: 'quero pegar emprestado', 'tem o livro X?', 'livros de direito disponíveis'.
        Considere títulos, autores, temas e cursos.
        """)
    public String buscarLivro(
            @ToolParam(description = "Título, autor, tema ou curso para buscar. Ex: 'Java', 'Clean Code', 'Programação'")
            String busca) {

        log.debug("[buscarLivro] Buscando por: {}", busca);

        List<LivroDocument> livros = livroDocumentRepository.buscarSemanticamente(busca);

        log.debug("[buscarLivro] Encontrados: {}", livros.size());

        if (livros.isEmpty()) {
            return "Nenhum livro encontrado no acervo para: " + busca;
        }

        return livros.stream()
                .map(livro -> """
                        Título: %s
                        Autor: %s
                        Descrição: %s
                        Cursos: %s
                        Temas: %s
                        """.formatted(
                        livro.getTitulo(),
                        livro.getAutor(),
                        livro.getDescricao() != null ? livro.getDescricao() : "Sem descrição",
                        livro.getCursos() != null
                                ? livro.getCursos().stream()
                                .map(CursoDocument::getNome)
                                .collect(Collectors.joining(", "))
                                : "Nenhum curso vinculado",
                        livro.getTemas() != null
                                ? livro.getTemas().stream()
                                .map(TemaDocument::getNome)
                                .collect(Collectors.joining(", "))
                                : "Nenhum tema vinculado"
                ))
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = """
        Busca livros de DOMÍNIO PÚBLICO, GRATUITOS ou disponíveis para DOWNLOAD no acervo.
        Use quando o usuário mencionar: baixar, download, gratuito, grátis, domínio público, livre, aberto, ler online, PDF.
        SEMPRE prefira esta ferramenta quando houver intenção de obter o livro digitalmente.
        Considere títulos, autores, temas e cursos.
        Exemplos: 'livro de direito para baixar', 'tem algum livro de Java?', 'livro gratuito de enfermagem'.
        """)
    public String buscarLivroPublico(
            @ToolParam(description = "Título, autor, tema ou curso para buscar. Ex: 'Java', 'Clean Code', 'Programação'")
            String busca) {

        log.debug("[buscarLivroPublico] Buscando por: {}", busca);

        List<LivroPublicoDocument> livros = livroPublicoDocumentRepository.buscarSemanticamente(busca);

        log.debug("[buscarLivroPublico] Encontrados: {}", livros.size());

        if (livros.isEmpty()) {
            return "Nenhum livro encontrado no acervo para: " + busca;
        }

        return livros.stream()
                .map(livro -> """
                        Título: %s
                        Autor: %s
                        Descrição: %s
                        Cursos: %s
                        Temas: %s
                        """.formatted(
                        livro.getTitulo(),
                        livro.getAutor(),
                        livro.getDescricao() != null ? livro.getDescricao() : "Sem descrição",
                        livro.getCursos() != null
                                ? livro.getCursos().stream()
                                .map(CursoDocument::getNome)
                                .collect(Collectors.joining(", "))
                                : "Nenhum curso vinculado",
                        livro.getTemas() != null
                                ? livro.getTemas().stream()
                                .map(TemaDocument::getNome)
                                .collect(Collectors.joining(", "))
                                : "Nenhum tema vinculado"
                ))
                .collect(Collectors.joining("\n---\n"));
    }

    @Tool(description = """
            Busca Trabalhos de Conclusão de Curso (TCCs) no acervo da biblioteca.
            Use quando o usuário mencionar: TCC, monografia, trabalho de conclusão, trabalho acadêmico, trabalho final.
            NÃO use para livros físicos, livros públicos ou materiais de estudo.
            Exemplos: 'TCC de direito', 'monografia sobre redes', 'trabalho de conclusão de enfermagem'.
            Considere títulos, autores, temas e curso.
            """)
    public String buscarTcc(
            @ToolParam(description = "Título, autor, tema ou curso. Ex: 'Direito', 'Inteligência Artificial', 'Enfermagem'")
            String busca) {

        log.debug("[buscarTcc] Buscando: {}", busca);

        List<TccDocument> tccs = tccDocumentRepository.buscarSemanticamente(busca);

        log.debug("[buscarTcc] Encontrados: {}", tccs.size());

        if (tccs.isEmpty()) return "Nenhum TCC encontrado no acervo para: " + busca;

        return tccs.stream()
                .map(tcc -> """
                        Título: %s
                        Autor: %s
                        Descrição: %s
                        Curso: %s
                        Temas: %s
                        """.formatted(
                        tcc.getTitulo(),
                        tcc.getAutor(),
                        tcc.getDescricao() != null ? tcc.getDescricao() : "Sem descrição",
                        tcc.getCurso() != null ? tcc.getCurso().getNome() : "Curso não informado",
                        tcc.getTemas() != null
                                ? tcc.getTemas().stream().map(TemaDocument::getNome).collect(Collectors.joining(", "))
                                : "Nenhum tema vinculado"
                ))
                .collect(Collectors.joining("\n---\n"));
    }


    @Tool(description = """
            Busca materiais de estudo no acervo da biblioteca.
            Use quando o usuário mencionar: material de estudo, apostila, resumo, slides, anotações, material didático, material de apoio.
            NÃO use para livros físicos, livros públicos ou TCCs.
            Exemplos: 'material de estudo de cálculo', 'apostila de direito', 'resumo de enfermagem'.
            Considere títulos, autores, temas e cursos.
            """)
    public String buscarMaterialEstudo(
            @ToolParam(description = "Título, autor, tema ou curso. Ex: 'Cálculo', 'Direito', 'Enfermagem'")
            String busca) {

        log.debug("[buscarMaterialEstudo] Buscando: {}", busca);

        List<MaterialEstudoDocument> materiais = materialEstudoDocumentRepository.buscarSemanticamente(busca);

        log.debug("[buscarMaterialEstudo] Encontrados: {}", materiais.size());

        if (materiais.isEmpty()) return "Nenhum material de estudo encontrado no acervo para: " + busca;

        return materiais.stream()
                .map(material -> """
                        Título: %s
                        Autor: %s
                        Descrição: %s
                        Cursos: %s
                        Temas: %s
                        """.formatted(
                        material.getTitulo(),
                        material.getAutor(),
                        material.getDescricao() != null ? material.getDescricao() : "Sem descrição",
                        material.getCursos() != null
                                ? material.getCursos().stream().map(CursoDocument::getNome).collect(Collectors.joining(", "))
                                : "Nenhum curso vinculado",
                        material.getTemas() != null
                                ? material.getTemas().stream().map(TemaDocument::getNome).collect(Collectors.joining(", "))
                                : "Nenhum tema vinculado"
                ))
                .collect(Collectors.joining("\n---\n"));
    }
}
