package com.digital.smartteca.search.repository.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.repository.LivroBuscaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Ate a Fase 2, essa busca so existia por dentro do chatbot (RAG tool em
 * MetodosChat, que ainda vive no core-service) — nunca foi exposta via
 * REST. O endpoint /buscar do api-gateway ja apontava pra essa rota, so
 * nao existia nada respondendo. Portado 1:1 da query original, so
 * trocando o pacote.
 */
@Repository
@RequiredArgsConstructor
public class LivroBuscaRepositoryImpl implements LivroBuscaRepository {

    private final ElasticsearchOperations operations;

    @Override
    public List<LivroDocument> buscarSemanticamente(String texto) {

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
                                        .nested(n -> n
                                                .path("cursos")
                                                .query(nq -> nq
                                                        .match(m -> m
                                                                .field("cursos.nome")
                                                                .query(texto)
                                                                .fuzziness("AUTO")
                                                        )
                                                )
                                                .scoreMode(ChildScoreMode.Max)
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

        return operations.search(query, LivroDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();

    }
}
