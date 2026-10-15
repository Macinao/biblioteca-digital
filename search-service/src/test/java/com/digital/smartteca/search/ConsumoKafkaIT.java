package com.digital.smartteca.search;

import com.digital.smartteca.common.enums.StatusReserva;
import com.digital.smartteca.common.events.DomainEvent;
import com.digital.smartteca.search.document.CursoDocument;
import com.digital.smartteca.search.document.LivroDocument;
import com.digital.smartteca.search.document.LivroPublicoDocument;
import com.digital.smartteca.search.document.MaterialEstudoDocument;
import com.digital.smartteca.search.document.ReservaDocument;
import com.digital.smartteca.search.document.TccDocument;
import com.digital.smartteca.search.document.TemaDocument;
import com.digital.smartteca.search.repository.CursoDocumentRepository;
import com.digital.smartteca.search.repository.LivroDocumentRepository;
import com.digital.smartteca.search.repository.LivroPublicoDocumentRepository;
import com.digital.smartteca.search.repository.MaterialEstudoDocumentRepository;
import com.digital.smartteca.search.repository.ReservaDocumentRepository;
import com.digital.smartteca.search.repository.TccDocumentRepository;
import com.digital.smartteca.search.repository.TemaDocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Kafka real -> consumers -> Elasticsearch real: o caminho que mantem o modelo de leitura sincronizado com o core-service. */
class ConsumoKafkaIT extends SearchBase {

    static KafkaProducer<String, String> producer;

    @Autowired ObjectMapper objectMapper;
    @Autowired CursoDocumentRepository cursos;
    @Autowired TemaDocumentRepository temas;
    @Autowired LivroDocumentRepository livros;
    @Autowired LivroPublicoDocumentRepository livrosPublicos;
    @Autowired TccDocumentRepository tccs;
    @Autowired MaterialEstudoDocumentRepository materiais;
    @Autowired ReservaDocumentRepository reservas;

    @AfterAll
    static void fechar() {
        if (producer != null) {
            producer.close();
        }
    }

    private KafkaProducer<String, String> producer() {
        if (producer == null) {
            producer = new KafkaProducer<>(Map.of(
                    "bootstrap.servers", KAFKA.getBootstrapServers(),
                    "key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
                    "value.serializer", "org.apache.kafka.common.serialization.StringSerializer"));
        }
        return producer;
    }

    private void publicar(String topico, String tipo, String aggregateId, String payload) throws Exception {
        DomainEvent evento = new DomainEvent(UUID.randomUUID().toString(), "Agregado", aggregateId, tipo, payload, Instant.now(), "corr-it");
        producer().send(new ProducerRecord<>(topico, aggregateId, objectMapper.writeValueAsString(evento))).get();
    }

    private void publicarCru(String topico, String chave, String valor) throws Exception {
        producer().send(new ProducerRecord<>(topico, chave, valor)).get();
    }

    private static void aguardar(Runnable verificacao) {
        await().atMost(Duration.ofSeconds(40)).pollInterval(Duration.ofMillis(300)).untilAsserted(verificacao::run);
    }

    private static String id() {
        return UUID.randomUUID().toString();
    }

    // =====================  CICLO DE VIDA POR AGREGADO  =====================

    @Test
    void cursoCadastradoAtualizadoEExcluidoRefleteNoIndice() throws Exception {
        String id = id();

        publicar("curso-events", "CursoCadastrado", id, "{\"id\":\"" + id + "\",\"nome\":\"Direito\"}");
        aguardar(() -> assertThat(cursos.findById(id)).get().extracting(CursoDocument::getNome).isEqualTo("Direito"));

        publicar("curso-events", "CursoAtualizado", id, "{\"id\":\"" + id + "\",\"nome\":\"Direito Penal\"}");
        aguardar(() -> assertThat(cursos.findById(id)).get().extracting(CursoDocument::getNome).isEqualTo("Direito Penal"));

        publicar("curso-events", "CursoExcluido", id, "{}");
        aguardar(() -> assertThat(cursos.findById(id)).isEmpty());
    }

    @Test
    void livroCadastradoEAtualizadoComNovoEstoqueEExcluido() throws Exception {
        String id = id();
        String base = "{\"id\":\"" + id + "\",\"titulo\":\"Clean Code\",\"autor\":\"Robert Martin\",\"quantidade\":%d,"
                + "\"dataPublicacao\":\"2020-01-01T00:00:00\",\"cursos\":[],\"temas\":[]}";

        publicar("livro-events", "LivroCadastrado", id, base.formatted(3));
        aguardar(() -> assertThat(livros.findById(id)).get().extracting(LivroDocument::getQuantidade).isEqualTo(3));

        // reservar/devolver publicam LivroAtualizado no mesmo topico com o novo estoque
        publicar("livro-events", "LivroAtualizado", id, base.formatted(2));
        aguardar(() -> assertThat(livros.findById(id)).get().extracting(LivroDocument::getQuantidade).isEqualTo(2));

        publicar("livro-events", "LivroExcluido", id, "{}");
        aguardar(() -> assertThat(livros.findById(id)).isEmpty());
    }

    @Test
    void livroPublicoTccEMaterialDeEstudoCicloCompleto() throws Exception {
        String publico = id();
        String tcc = id();
        String material = id();

        publicar("livro-publico-events", "LivroPublicoCadastrado", publico, "{\"id\":\"" + publico + "\",\"titulo\":\"Dom Casmurro\",\"capa\":\"c\",\"conteudo\":\"p\"}");
        publicar("tcc-events", "TccCadastrado", tcc, "{\"id\":\"" + tcc + "\",\"titulo\":\"Redes\",\"curso\":{\"id\":\"" + id() + "\",\"nome\":\"Computação\"}}");
        publicar("material-estudo-events", "MaterialEstudoCadastrado", material, "{\"id\":\"" + material + "\",\"titulo\":\"Lista 1\"}");

        aguardar(() -> {
            assertThat(livrosPublicos.findById(publico)).get().extracting(LivroPublicoDocument::getTitulo).isEqualTo("Dom Casmurro");
            assertThat(tccs.findById(tcc)).get().extracting(t -> t.getCurso().getNome()).isEqualTo("Computação");
            assertThat(materiais.findById(material)).get().extracting(MaterialEstudoDocument::getTitulo).isEqualTo("Lista 1");
        });

        publicar("livro-publico-events", "LivroPublicoExcluido", publico, "{}");
        publicar("tcc-events", "TccExcluido", tcc, "{}");
        publicar("material-estudo-events", "MaterialEstudoExcluido", material, "{}");
        aguardar(() -> {
            assertThat(livrosPublicos.findById(publico)).isEmpty();
            assertThat(tccs.findById(tcc)).isEmpty();
            assertThat(materiais.findById(material)).isEmpty();
        });
    }

    @Test
    void reservaCriadaViraAtrasadaEDevolvidaSomeDoIndice() throws Exception {
        String id = id();
        String base = "{\"id\":\"" + id + "\",\"matriculaUsuario\":\"2023001\",\"tituloLivro\":\"Clean Code\",\"statusReserva\":\"%s\"}";

        publicar("reserva-events", "ReservaCriada", id, base.formatted("RESERVADO"));
        aguardar(() -> assertThat(reservas.findById(id)).get().extracting(ReservaDocument::getStatusReserva).isEqualTo(StatusReserva.RESERVADO));

        publicar("reserva-events", "ReservaAtrasada", id, base.formatted("ATRASADO"));
        aguardar(() -> assertThat(reservas.findById(id)).get().extracting(ReservaDocument::getStatusReserva).isEqualTo(StatusReserva.ATRASADO));

        publicar("reserva-events", "ReservaDevolvida", id, "{}");
        aguardar(() -> assertThat(reservas.findById(id)).isEmpty());
    }

    // =====================  FAN-OUT  =====================

    @Test
    void renomearCursoAtualizaACopiaEmbutidaNosLivrosETccsJaIndexados() throws Exception {
        String cursoId = id();
        String outroCurso = id();
        LivroDocument livro = new LivroDocument();
        livro.setId(id());
        livro.setTitulo("Livro de Direito");
        livro.setCursos(List.of(curso(cursoId, "Direito"), curso(outroCurso, "Medicina")));
        livros.save(livro);
        TccDocument tcc = new TccDocument();
        tcc.setId(id());
        tcc.setTitulo("TCC de Direito");
        tcc.setCurso(curso(cursoId, "Direito"));
        tccs.save(tcc);
        TccDocument deOutro = new TccDocument();
        deOutro.setId(id());
        deOutro.setTitulo("TCC de Medicina");
        deOutro.setCurso(curso(outroCurso, "Medicina"));
        tccs.save(deOutro);
        refresh(LivroDocument.class, TccDocument.class);

        publicar("curso-events", "CursoAtualizado", cursoId, "{\"id\":\"" + cursoId + "\",\"nome\":\"Direito Penal\"}");

        aguardar(() -> {
            assertThat(livros.findById(livro.getId())).get().extracting(l -> l.getCursos().stream().map(CursoDocument::getNome).toList())
                    .isEqualTo(List.of("Direito Penal", "Medicina"));
            assertThat(tccs.findById(tcc.getId())).get().extracting(t -> t.getCurso().getNome()).isEqualTo("Direito Penal");
        });
        assertThat(tccs.findById(deOutro.getId())).get().extracting(t -> t.getCurso().getNome()).isEqualTo("Medicina");
    }

    @Test
    void renomearTemaAtualizaACopiaEmbutidaNosMateriaisEmLivrosPublicos() throws Exception {
        String temaId = id();
        MaterialEstudoDocument material = new MaterialEstudoDocument();
        material.setId(id());
        material.setTitulo("Material");
        material.setTemas(List.of(tema(temaId, "Algoritmos")));
        materiais.save(material);
        LivroPublicoDocument publico = new LivroPublicoDocument();
        publico.setId(id());
        publico.setTitulo("Publico");
        publico.setTemas(List.of(tema(temaId, "Algoritmos"), tema(id(), "Redes")));
        livrosPublicos.save(publico);
        refresh(MaterialEstudoDocument.class, LivroPublicoDocument.class);

        publicar("tema-events", "TemaAtualizado", temaId, "{\"id\":\"" + temaId + "\",\"nome\":\"Estruturas de Dados\"}");

        aguardar(() -> {
            assertThat(materiais.findById(material.getId())).get().extracting(m -> m.getTemas().get(0).getNome()).isEqualTo("Estruturas de Dados");
            assertThat(livrosPublicos.findById(publico.getId())).get().extracting(p -> p.getTemas().stream().map(TemaDocument::getNome).toList())
                    .isEqualTo(List.of("Estruturas de Dados", "Redes"));
        });
    }

    // =====================  ROBUSTEZ  =====================

    @Test
    void entregaDuplicadaDoMesmoEventoNaoDuplicaODocumento() throws Exception {
        String id = id();
        String payload = "{\"id\":\"" + id + "\",\"nome\":\"Direito\"}";

        publicar("curso-events", "CursoCadastrado", id, payload);
        publicar("curso-events", "CursoCadastrado", id, payload);
        publicar("curso-events", "CursoCadastrado", id, payload);

        aguardar(() -> assertThat(cursos.findById(id)).isPresent());
        refresh(CursoDocument.class);
        assertThat(cursos.count()).isEqualTo(1);
    }

    @Test
    void eventosDoMesmoAgregadoSaoAplicadosNaOrdemEmQueForamPublicados() throws Exception {
        String id = id();
        for (int i = 1; i <= 10; i++) {
            publicar("curso-events", i == 1 ? "CursoCadastrado" : "CursoAtualizado", id, "{\"id\":\"" + id + "\",\"nome\":\"Versao " + i + "\"}");
        }

        aguardar(() -> assertThat(cursos.findById(id)).get().extracting(CursoDocument::getNome).isEqualTo("Versao 10"));
    }

    @Test
    void mensagemVenenosaNaoBloqueiaAsSeguintes() throws Exception {
        String id = id();
        publicarCru("curso-events", "k", "isto nao e json");
        publicarCru("curso-events", "k", "{\"eventType\":\"CursoCadastrado\",\"payload\":\"payload-quebrado\"}");
        publicar("curso-events", "CursoCadastrado", id, "{\"id\":\"" + id + "\",\"nome\":\"Chegou depois do veneno\"}");

        aguardar(() -> assertThat(cursos.findById(id)).get().extracting(CursoDocument::getNome).isEqualTo("Chegou depois do veneno"));
    }

    @Test
    void tipoDeEventoDesconhecidoEIgnoradoSemAfetarOsDemais() throws Exception {
        String id = id();
        publicar("livro-events", "LivroTeletransportado", id(), "{}");
        publicar("curso-events", "CursoCadastrado", id, "{\"id\":\"" + id + "\",\"nome\":\"Normal\"}");

        aguardar(() -> assertThat(cursos.findById(id)).isPresent());
    }

    @Test
    void campoNovoNoPayloadDoCoreNaoDerrubaOConsumo() throws Exception {
        String id = id();

        publicar("curso-events", "CursoCadastrado", id, "{\"id\":\"" + id + "\",\"nome\":\"Com campo novo\",\"campoDoFuturo\":42}");

        aguardar(() -> assertThat(cursos.findById(id)).get().extracting(CursoDocument::getNome).isEqualTo("Com campo novo"));
    }

    @Test
    void excluirDocumentoQueNaoExisteNaoQuebraOConsumidor() throws Exception {
        String id = id();
        publicar("curso-events", "CursoExcluido", id(), "{}");
        publicar("curso-events", "CursoCadastrado", id, "{\"id\":\"" + id + "\",\"nome\":\"Depois\"}");

        aguardar(() -> assertThat(cursos.findById(id)).isPresent());
    }
}
