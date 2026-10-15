package com.digital.smartteca.storage.service;

import com.digital.smartteca.common.enums.BucketSupabase;
import com.digital.smartteca.storage.SupabaseFake;
import com.digital.smartteca.storage.config.SupabaseProperties;
import com.digital.smartteca.storage.config.WebClientConfig;
import com.digital.smartteca.storage.treatment.IntegracaoExternaException;
import com.digital.smartteca.storage.treatment.RecursoNaoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cliente do Supabase Storage contra um servidor HTTP real (fake). */
class SupabaseServiceImplTest {

    SupabaseFake supabase;
    SupabaseServiceImpl service;

    @BeforeEach
    void setUp() {
        supabase = new SupabaseFake();
        service = servicoApontandoPara(supabase.url());
    }

    @AfterEach
    void tearDown() {
        supabase.parar();
    }

    private SupabaseServiceImpl servicoApontandoPara(String url) {
        SupabaseProperties props = new SupabaseProperties();
        props.setUrl(url);
        props.setKey("chave-do-supabase");
        return new SupabaseServiceImpl(new WebClientConfig(props).supabaseWebClient(), props);
    }

    private static byte[] lerCorpo(ResponseEntity<Flux<DataBuffer>> resposta) {
        return DataBufferUtils.join(resposta.getBody()).map(buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            return bytes;
        }).block();
    }

    // ---------- subir ----------

    @Test
    void subirFazPostNoBucketCertoComChavesEUpsertEConteudoIntacto() {
        byte[] conteudo = "%PDF-conteudo".getBytes(StandardCharsets.UTF_8);

        service.subir(conteudo, "application/pdf", BucketSupabase.TCC_CONTEUDO, "tcc-1").block();

        var recebida = supabase.recebidas.peek();
        assertThat(recebida.metodo()).isEqualTo("POST");
        assertThat(recebida.caminho()).isEqualTo("/storage/v1/object/tcc-conteudo/tcc-1");
        assertThat(recebida.authorization()).isEqualTo("Bearer chave-do-supabase");
        assertThat(recebida.apikey()).isEqualTo("chave-do-supabase");
        assertThat(recebida.upsert()).isEqualTo("true");
        assertThat(recebida.contentType()).isEqualTo("application/pdf");
        assertThat(recebida.corpo()).isEqualTo(conteudo);
    }

    @Test
    void subirUsaONomeDoBucketDoEnum() {
        service.subir(new byte[]{1}, "image/png", BucketSupabase.LIVRO_PUBLICO_CAPA, "x").block();

        assertThat(supabase.recebidas.peek().caminho()).isEqualTo("/storage/v1/object/livro-publico-capa/x");
    }

    @Test
    void subirComStatusDeErroViraIntegracaoExternaComOStatusNaMensagem() {
        supabase.statusForcado = 500;

        assertThatThrownBy(() -> service.subir(new byte[]{1}, "image/png", BucketSupabase.LIVRO_CAPA, "x").block())
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessageContaining("Falha ao subir arquivo para o Supabase")
                .hasMessageContaining("500");
    }

    @Test
    void subirComSupabaseForaDoArViraIntegracaoExterna() {
        supabase.parar();

        assertThatThrownBy(() -> service.subir(new byte[]{1}, "image/png", BucketSupabase.LIVRO_CAPA, "x").block())
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessageContaining("Falha ao comunicar com o Supabase");
    }

    // ---------- baixar ----------

    @Test
    void baixarDevolveOArquivoComContentTypeEContentDispositionInline() {
        byte[] conteudo = "bytes-da-capa".getBytes(StandardCharsets.UTF_8);
        service.subir(conteudo, "image/png", BucketSupabase.LIVRO_CAPA, "capa-1").block();

        ResponseEntity<Flux<DataBuffer>> resposta = service.baixar("capa-1", BucketSupabase.LIVRO_CAPA).block();

        assertThat(resposta.getStatusCode().value()).isEqualTo(200);
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("inline; filename=\"capa-1\"");
        assertThat(lerCorpo(resposta)).isEqualTo(conteudo);
    }

    @Test
    void baixarEnviaAsCredenciaisDoSupabaseNoGet() {
        service.subir(new byte[]{1}, "image/png", BucketSupabase.LIVRO_CAPA, "capa-1").block();
        supabase.recebidas.clear();

        lerCorpo(service.baixar("capa-1", BucketSupabase.LIVRO_CAPA).block());

        var recebida = supabase.recebidas.peek();
        assertThat(recebida.metodo()).isEqualTo("GET");
        assertThat(recebida.authorization()).isEqualTo("Bearer chave-do-supabase");
        assertThat(recebida.apikey()).isEqualTo("chave-do-supabase");
    }

    @Test
    void baixarArquivoInexistenteViraRecursoNaoEncontrado() {
        assertThatThrownBy(() -> service.baixar("nao-existe", BucketSupabase.LIVRO_CAPA).block())
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Arquivo não encontrado no Supabase");
    }

    @Test
    void baixarComQualquerErroDoSupabaseTambemViraNaoEncontrado() {
        // Comportamento atual: 4xx e 5xx do Supabase sao tratados igual (404 para o cliente).
        supabase.statusForcado = 500;

        assertThatThrownBy(() -> service.baixar("x", BucketSupabase.LIVRO_CAPA).block())
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void baixarComSupabaseForaDoArViraIntegracaoExterna() {
        supabase.parar();

        assertThatThrownBy(() -> service.baixar("x", BucketSupabase.LIVRO_CAPA).block())
                .isInstanceOf(IntegracaoExternaException.class);
    }

    @Test
    void baixarArquivoGrandeChegaCompletoEmStreaming() {
        byte[] grande = new byte[3 * 1024 * 1024];
        new java.util.Random(42).nextBytes(grande);
        service.subir(grande, "application/pdf", BucketSupabase.TCC_CONTEUDO, "grande").block();

        ResponseEntity<Flux<DataBuffer>> resposta = service.baixar("grande", BucketSupabase.TCC_CONTEUDO).block();

        assertThat(lerCorpo(resposta)).isEqualTo(grande);
    }

    // ---------- deletar ----------

    @Test
    void deletarFazDeleteNoCaminhoDoBucketComCredenciais() {
        service.subir(new byte[]{1}, "image/png", BucketSupabase.LIVRO_CAPA, "capa-1").block();
        supabase.recebidas.clear();

        service.deletar("capa-1", BucketSupabase.LIVRO_CAPA).block();

        var recebida = supabase.recebidas.peek();
        assertThat(recebida.metodo()).isEqualTo("DELETE");
        assertThat(recebida.caminho()).isEqualTo("/storage/v1/object/livro-capa/capa-1");
        assertThat(recebida.authorization()).isEqualTo("Bearer chave-do-supabase");
        assertThat(supabase.arquivos).isEmpty();
    }

    @Test
    void deletarArquivoInexistenteViraIntegracaoExterna() {
        assertThatThrownBy(() -> service.deletar("nao-existe", BucketSupabase.LIVRO_CAPA).block())
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessageContaining("Falha ao deletar arquivo no Supabase")
                .hasMessageContaining("404");
    }

    @Test
    void deletarComSupabaseForaDoArViraIntegracaoExterna() {
        supabase.parar();

        assertThatThrownBy(() -> service.deletar("x", BucketSupabase.LIVRO_CAPA).block())
                .isInstanceOf(IntegracaoExternaException.class);
    }
}
