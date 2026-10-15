package com.digital.smartteca.search.consumer;

import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.common.observability.CorrelationId;
import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.document.TemaDocument;
import com.digital.smartteca.search.fanout.CursoFanOutService;
import com.digital.smartteca.search.fanout.TemaFanOutService;
import com.digital.smartteca.search.repository.CursoDocumentRepository;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.repository.ReservaDocumentRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import com.digital.smartteca.search.repository.TemaDocumentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/** Os 7 consumers Kafka seguem o mesmo contrato: upsert no cadastro/atualizacao, delete pelo aggregateId, ignora tipo desconhecido. */
class ConsumersUnitTest {

    static final ObjectMapper MAPPER = new com.digital.smartteca.search.config.JacksonConfig().objectMapper();

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    static String mensagem(String tipo, String aggregateId, String payload, String correlationId) throws Exception {
        return MAPPER.writeValueAsString(new DomainEvent("evt-1", "Agregado", aggregateId, tipo, payload, Instant.parse("2026-01-01T00:00:00Z"), correlationId));
    }

    static String mensagem(String tipo, String aggregateId, String payload) throws Exception {
        return mensagem(tipo, aggregateId, payload, "corr-1");
    }

    // =====================  CURSO  =====================

    @Nested
    class Curso {
        final CursoDocumentRepository repo = mock(CursoDocumentRepository.class);
        final CursoFanOutService fanOut = mock(CursoFanOutService.class);
        final CursoEventConsumer consumer = new CursoEventConsumer(repo, fanOut, MAPPER);

        @Test
        void cadastradoFazUpsertSemFanOut() throws Exception {
            consumer.consumir(mensagem("CursoCadastrado", "c1", "{\"id\":\"c1\",\"nome\":\"Direito\"}"));

            ArgumentCaptor<CursoDocument> salvo = ArgumentCaptor.forClass(CursoDocument.class);
            verify(repo).save(salvo.capture());
            assertThat(salvo.getValue().getId()).isEqualTo("c1");
            assertThat(salvo.getValue().getNome()).isEqualTo("Direito");
            verifyNoInteractions(fanOut);
        }

        @Test
        void atualizadoFazUpsertEPropagaARenomeacaoParaAsCopiasEmbutidas() throws Exception {
            consumer.consumir(mensagem("CursoAtualizado", "c1", "{\"id\":\"c1\",\"nome\":\"Direito Penal\"}"));

            ArgumentCaptor<CursoDocument> salvo = ArgumentCaptor.forClass(CursoDocument.class);
            verify(repo).save(salvo.capture());
            verify(fanOut).propagarAtualizacao(salvo.getValue());
            assertThat(salvo.getValue().getNome()).isEqualTo("Direito Penal");
        }

        @Test
        void excluidoRemovePeloAggregateIdSemFanOut() throws Exception {
            consumer.consumir(mensagem("CursoExcluido", "c1", "{}"));

            verify(repo).deleteById("c1");
            verifyNoInteractions(fanOut);
        }

        @Test
        void tipoDesconhecidoEIgnorado() throws Exception {
            consumer.consumir(mensagem("CursoVirouOutraCoisa", "c1", "{}"));

            verifyNoInteractions(repo, fanOut);
        }

        @Test
        void jsonInvalidoLancaExcecaoParaOKafkaTratar() {
            assertThatThrownBy(() -> consumer.consumir("isto nao e json")).isInstanceOf(JsonProcessingException.class);

            verifyNoInteractions(repo, fanOut);
        }

        @Test
        void payloadInvalidoDentroDoEnvelopeLancaExcecaoSemSalvar() {
            assertThatThrownBy(() -> consumer.consumir(mensagem("CursoCadastrado", "c1", "nao-e-json")))
                    .isInstanceOf(JsonProcessingException.class);

            verify(repo, never()).save(any());
        }

        @Test
        void correlationIdDoEventoFicaNoMdcDuranteOProcessamentoESaiDepois() throws Exception {
            AtomicReference<String> mdc = new AtomicReference<>();
            doAnswer(inv -> {
                mdc.set(MDC.get(CorrelationId.MDC_KEY));
                return null;
            }).when(repo).save(any());

            consumer.consumir(mensagem("CursoCadastrado", "c1", "{\"id\":\"c1\",\"nome\":\"X\"}", "corr-do-evento"));

            assertThat(mdc.get()).isEqualTo("corr-do-evento");
            assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
        }

        @Test
        void mdcLimpoMesmoQuandoOProcessamentoFalha() {
            doAnswer(inv -> {
                throw new IllegalStateException("es fora");
            }).when(repo).save(any());

            assertThatThrownBy(() -> consumer.consumir(mensagem("CursoCadastrado", "c1", "{\"id\":\"c1\"}", "corr-x")))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
        }
    }

    // =====================  TEMA  =====================

    @Nested
    class Tema {
        final TemaDocumentRepository repo = mock(TemaDocumentRepository.class);
        final TemaFanOutService fanOut = mock(TemaFanOutService.class);
        final TemaEventConsumer consumer = new TemaEventConsumer(repo, fanOut, MAPPER);

        @Test
        void cadastradoFazUpsertSemFanOut() throws Exception {
            consumer.consumir(mensagem("TemaCadastrado", "t1", "{\"id\":\"t1\",\"nome\":\"Algoritmos\"}"));

            verify(repo).save(any(TemaDocument.class));
            verifyNoInteractions(fanOut);
        }

        @Test
        void atualizadoFazUpsertEPropagaParaAsCopias() throws Exception {
            consumer.consumir(mensagem("TemaAtualizado", "t1", "{\"id\":\"t1\",\"nome\":\"Estruturas\"}"));

            ArgumentCaptor<TemaDocument> salvo = ArgumentCaptor.forClass(TemaDocument.class);
            verify(repo).save(salvo.capture());
            verify(fanOut).propagarAtualizacao(salvo.getValue());
        }

        @Test
        void excluidoRemovePeloAggregateId() throws Exception {
            consumer.consumir(mensagem("TemaExcluido", "t1", "{}"));

            verify(repo).deleteById("t1");
        }

        @Test
        void tipoDesconhecidoEJsonInvalido() throws Exception {
            consumer.consumir(mensagem("Outro", "t1", "{}"));
            verifyNoInteractions(repo, fanOut);
            assertThatThrownBy(() -> consumer.consumir("{{")).isInstanceOf(JsonProcessingException.class);
        }
    }

    // =====================  LIVRO  =====================

    @Nested
    class Livro {
        final LivroDocumentRepository repo = mock(LivroDocumentRepository.class);
        final LivroEventConsumer consumer = new LivroEventConsumer(repo, MAPPER);

        @Test
        void cadastradoEAtualizadoFazemUpsertDoMesmoJeito() throws Exception {
            String payload = "{\"id\":\"l1\",\"titulo\":\"Clean Code\",\"quantidade\":3,\"dataPublicacao\":\"2020-01-01T00:00:00\","
                    + "\"cursos\":[{\"id\":\"c1\",\"nome\":\"Computação\"}],\"temas\":[{\"id\":\"t1\",\"nome\":\"Software\"}]}";

            consumer.consumir(mensagem("LivroCadastrado", "l1", payload));
            consumer.consumir(mensagem("LivroAtualizado", "l1", payload));

            ArgumentCaptor<LivroDocument> salvos = ArgumentCaptor.forClass(LivroDocument.class);
            verify(repo, org.mockito.Mockito.times(2)).save(salvos.capture());
            LivroDocument doc = salvos.getAllValues().get(0);
            assertThat(doc.getTitulo()).isEqualTo("Clean Code");
            assertThat(doc.getQuantidade()).isEqualTo(3);
            assertThat(doc.getCursos()).extracting(CursoDocument::getNome).containsExactly("Computação");
            assertThat(doc.getTemas()).extracting(TemaDocument::getNome).containsExactly("Software");
            assertThat(doc.getDataPublicacao()).hasYear(2020);
        }

        @Test
        void excluidoRemovePeloAggregateId() throws Exception {
            consumer.consumir(mensagem("LivroExcluido", "l1", "{}"));

            verify(repo).deleteById("l1");
        }

        @Test
        void tipoDesconhecidoNaoFazNada() throws Exception {
            consumer.consumir(mensagem("LivroTeletransportado", "l1", "{}"));

            verifyNoInteractions(repo);
        }

        @Test
        void camposDesconhecidosNoPayloadNaoQuebramOConsumo() {
            // Ex.: o core-service passa a enviar um campo novo antes do search-service ser atualizado.
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                    consumer.consumir(mensagem("LivroCadastrado", "l1", "{\"id\":\"l1\",\"titulo\":\"A\",\"campoNovoDoFuturo\":123}")));
        }
    }

    // =====================  LIVRO PUBLICO  =====================

    @Nested
    class LivroPublico {
        final LivroPublicoDocumentRepository repo = mock(LivroPublicoDocumentRepository.class);
        final LivroPublicoEventConsumer consumer = new LivroPublicoEventConsumer(repo, MAPPER);

        @Test
        void cadastradoEAtualizadoFazemUpsertEExcluidoRemove() throws Exception {
            String payload = "{\"id\":\"p1\",\"titulo\":\"Dom Casmurro\",\"capa\":\"p1\",\"conteudo\":\"p1\"}";

            consumer.consumir(mensagem("LivroPublicoCadastrado", "p1", payload));
            consumer.consumir(mensagem("LivroPublicoAtualizado", "p1", payload));
            consumer.consumir(mensagem("LivroPublicoExcluido", "p1", "{}"));

            verify(repo, org.mockito.Mockito.times(2)).save(any(LivroPublicoDocument.class));
            verify(repo).deleteById("p1");
        }

        @Test
        void tipoDesconhecidoEIgnorado() throws Exception {
            consumer.consumir(mensagem("Qualquer", "p1", "{}"));

            verifyNoInteractions(repo);
        }
    }

    // =====================  MATERIAL DE ESTUDO  =====================

    @Nested
    class MaterialEstudo {
        final MaterialEstudoDocumentRepository repo = mock(MaterialEstudoDocumentRepository.class);
        final MaterialEstudoEventConsumer consumer = new MaterialEstudoEventConsumer(repo, MAPPER);

        @Test
        void cadastradoEAtualizadoFazemUpsertEExcluidoRemove() throws Exception {
            String payload = "{\"id\":\"m1\",\"titulo\":\"Lista 1\"}";

            consumer.consumir(mensagem("MaterialEstudoCadastrado", "m1", payload));
            consumer.consumir(mensagem("MaterialEstudoAtualizado", "m1", payload));
            consumer.consumir(mensagem("MaterialEstudoExcluido", "m1", "{}"));

            verify(repo, org.mockito.Mockito.times(2)).save(any(MaterialEstudoDocument.class));
            verify(repo).deleteById("m1");
        }

        @Test
        void tipoDesconhecidoEIgnorado() throws Exception {
            consumer.consumir(mensagem("Qualquer", "m1", "{}"));

            verifyNoInteractions(repo);
        }
    }

    // =====================  TCC  =====================

    @Nested
    class Tcc {
        final TccDocumentRepository repo = mock(TccDocumentRepository.class);
        final TccEventConsumer consumer = new TccEventConsumer(repo, MAPPER);

        @Test
        void cadastradoEAtualizadoFazemUpsertComCursoUnicoEExcluidoRemove() throws Exception {
            String payload = "{\"id\":\"x1\",\"titulo\":\"Redes\",\"curso\":{\"id\":\"c1\",\"nome\":\"Computação\"}}";

            consumer.consumir(mensagem("TccCadastrado", "x1", payload));
            consumer.consumir(mensagem("TccAtualizado", "x1", payload));
            consumer.consumir(mensagem("TccExcluido", "x1", "{}"));

            ArgumentCaptor<TccDocument> salvos = ArgumentCaptor.forClass(TccDocument.class);
            verify(repo, org.mockito.Mockito.times(2)).save(salvos.capture());
            assertThat(salvos.getValue().getCurso().getNome()).isEqualTo("Computação");
            verify(repo).deleteById("x1");
            verifyNoMoreInteractions(repo);
        }

        @Test
        void tipoDesconhecidoEIgnorado() throws Exception {
            consumer.consumir(mensagem("Qualquer", "x1", "{}"));

            verifyNoInteractions(repo);
        }
    }

    // =====================  RESERVA  =====================

    @Nested
    class Reserva {
        final ReservaDocumentRepository repo = mock(ReservaDocumentRepository.class);
        final ReservaEventConsumer consumer = new ReservaEventConsumer(repo, MAPPER);

        @Test
        void criadaEAtrasadaFazemUpsertComOStatusDoPayload() throws Exception {
            consumer.consumir(mensagem("ReservaCriada", "r1", "{\"id\":\"r1\",\"matriculaUsuario\":\"2023001\",\"statusReserva\":\"RESERVADO\"}"));
            consumer.consumir(mensagem("ReservaAtrasada", "r1", "{\"id\":\"r1\",\"matriculaUsuario\":\"2023001\",\"statusReserva\":\"ATRASADO\"}"));

            ArgumentCaptor<ReservaDocument> salvos = ArgumentCaptor.forClass(ReservaDocument.class);
            verify(repo, org.mockito.Mockito.times(2)).save(salvos.capture());
            assertThat(salvos.getAllValues()).extracting(r -> r.getStatusReserva().name()).containsExactly("RESERVADO", "ATRASADO");
        }

        @Test
        void devolvidaRemoveODocumentoEmVezDeSoMudarOStatus() throws Exception {
            consumer.consumir(mensagem("ReservaDevolvida", "r1", "{}"));

            verify(repo).deleteById("r1");
            verify(repo, never()).save(any());
        }

        @Test
        void tipoDesconhecidoEIgnorado() throws Exception {
            consumer.consumir(mensagem("Qualquer", "r1", "{}"));

            verifyNoInteractions(repo);
        }
    }
}
