package com.digital.biblioteca.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.digital.biblioteca.document.TccDocument;
import com.digital.biblioteca.repository.TccBuscaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TccBuscaRepositoryImpl implements TccBuscaRepository {

    private final ElasticsearchOperations operations;

    @Override
    public List<TccDocument> buscarSemanticamente(String texto) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .should(s -> s
                                        .multiMatch(mm -> mm
                                                .query(texto)
                                                .fields(
                                                        "titulo^4",
                                                        "autor^3",
                                                        "descricao^1"
                                                )
                                                .fuzziness("AUTO")
                                                .operator(Operator.Or)
                                        )
                                )
                                .should(s -> s
                                        .match(m -> m
                                                .field("curso.nome")
                                                .query(texto)
                                                .fuzziness("AUTO")
                                                .boost(5.0F)
                                        )
                                )
                                .should(s -> s
                                        .nested(n -> n
                                                .path("temas")
                                                .query(nq -> nq
                                                        .match(m -> m
                                                                .field("temas.nome")
                                                                .query(texto)
                                                                .fuzziness("AUTO")
                                                        )
                                                )
                                                .scoreMode(ChildScoreMode.Max)
                                        )
                                )
                                .minimumShouldMatch("1")
                        )
                )
                .withMaxResults(20)
                .build();

        return operations.search(query, TccDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}
