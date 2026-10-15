package com.digital.smartteca.search.controller;

import com.digital.smartteca.search.treatment.RequisicaoInvalidaException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.function.Function;

/**
 * Paginação das listagens. O corpo da resposta continua sendo uma lista (o contrato de antes não quebra) e o
 * total de itens vai no header X-Total-Count, para o cliente saber quantas páginas existem.
 *
 * Por que paginar: a listagem antiga trazia o índice inteiro. Medido com 3 mil livros: 1,7 MB por chamada e
 * 30 mil nós de DOM no navegador; passando de 10 mil documentos o Elasticsearch recusava a consulta
 * (index.max_result_window) e a API devolvia erro.
 */
public final class Paginacao {

    public static final String HEADER_TOTAL = "X-Total-Count";

    static final int TAMANHO_PADRAO = 24;
    static final int TAMANHO_MAXIMO = 100;
    /** Limite do Elasticsearch para "pular" resultados (index.max_result_window). Além dele, só refinando a busca. */
    static final int JANELA_MAXIMA = 10_000;

    private Paginacao() {
    }

    /** Valida os parâmetros (página começa em 0) e ordena do mais recente para o mais antigo. */
    public static Pageable de(Integer pagina, Integer tamanho, String campoDeData) {
        int p = pagina == null ? 0 : pagina;
        int t = tamanho == null ? TAMANHO_PADRAO : tamanho;

        if (p < 0 || t < 1 || t > TAMANHO_MAXIMO) {
            throw new RequisicaoInvalidaException("Use pagina >= 0 e tamanho entre 1 e " + TAMANHO_MAXIMO + ".");
        }
        if ((long) (p + 1) * t > JANELA_MAXIMA) {
            throw new RequisicaoInvalidaException(
                    "A paginação alcança só os primeiros " + JANELA_MAXIMA + " resultados. Use a busca ou um filtro para refinar.");
        }
        return PageRequest.of(p, t, Sort.by(Sort.Direction.DESC, campoDeData));
    }

    public static <D, R> ResponseEntity<List<R>> resposta(Page<D> pagina, Function<List<D>, List<R>> conversor) {
        return ResponseEntity.ok()
                .header(HEADER_TOTAL, String.valueOf(pagina.getTotalElements()))
                .body(conversor.apply(pagina.getContent()));
    }
}
