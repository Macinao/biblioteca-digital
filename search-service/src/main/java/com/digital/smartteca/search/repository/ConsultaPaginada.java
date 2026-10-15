package com.digital.smartteca.search.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

/**
 * Listagem paginada, com filtro opcional por curso, para qualquer índice.
 *
 * Existe porque findAll(Pageable) do Spring Data não liga o track_total_hits do Elasticsearch: o total ficava
 * travado em 10000 mesmo com 11 mil documentos, e o front calcularia páginas erradas. Aqui o total é exato.
 */
@Component
@RequiredArgsConstructor
public class ConsultaPaginada {

    private final ElasticsearchOperations operations;

    /**
     * @param caminhoCurso campo do curso no documento ("cursos" ou "curso"); ignorado sem cursoId
     * @param cursoAninhado true quando o campo é uma lista nested (livro, livro público, material); false no TCC,
     *                      que tem um curso só, como objeto simples
     * @param cursoId filtro opcional (id exato do curso)
     */
    public <D> Page<D> listar(Class<D> tipo, Pageable pageable, String caminhoCurso, boolean cursoAninhado, String cursoId) {

        NativeQueryBuilder consulta = NativeQuery.builder()
                .withPageable(pageable)
                .withTrackTotalHits(true);

        if (cursoId == null || cursoId.isBlank()) {
            consulta.withQuery(q -> q.matchAll(m -> m));
        } else if (cursoAninhado) {
            consulta.withQuery(q -> q.nested(n -> n
                    .path(caminhoCurso)
                    .query(nq -> nq.term(t -> t.field(caminhoCurso + ".id.keyword").value(cursoId)))
                    .scoreMode(ChildScoreMode.None)));
        } else {
            consulta.withQuery(q -> q.term(t -> t.field(caminhoCurso + ".id.keyword").value(cursoId)));
        }

        SearchHits<D> resultado = operations.search(consulta.build(), tipo);
        return new PageImpl<>(resultado.stream().map(SearchHit::getContent).toList(), pageable, resultado.getTotalHits());
    }

    /** Sem filtro de curso (reservas). */
    public <D> Page<D> listar(Class<D> tipo, Pageable pageable) {
        return listar(tipo, pageable, null, false, null);
    }
}
