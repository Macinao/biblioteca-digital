package com.digital.biblioteca.validation;

import com.digital.biblioteca.validation.validator.ValidarImagem;
import com.digital.biblioteca.validation.validator.ValidarImagemCont;
import com.digital.biblioteca.validation.validator.ValidarPdf;
import com.digital.biblioteca.validation.validator.ValidarPdfCont;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * As quatro anotacoes (Imagem, ImagemCont, Pdf, PdfCont) diferem em um unico ponto: o arquivo
 * ausente e valido nas opcionais (Imagem/Pdf) e invalido nas obrigatorias (ImagemCont/PdfCont).
 */
class ValidadoresDeArquivoTest {

    final ValidarImagem imagemOpcional = new ValidarImagem();
    final ValidarImagemCont imagemObrigatoria = new ValidarImagemCont();
    final ValidarPdf pdfOpcional = new ValidarPdf();
    final ValidarPdfCont pdfObrigatorio = new ValidarPdfCont();

    private static MultipartFile arquivo(String nome, String tipo) {
        return new MockMultipartFile("arquivo", nome, tipo, "conteudo".getBytes());
    }

    // ---------- arquivo ausente / vazio ----------

    @Test
    void nuloEVazioSaoValidosNosValidadoresOpcionais() {
        MultipartFile vazio = new MockMultipartFile("arquivo", "capa.png", "image/png", new byte[0]);

        assertThat(imagemOpcional.isValid(null, null)).isTrue();
        assertThat(imagemOpcional.isValid(vazio, null)).isTrue();
        assertThat(pdfOpcional.isValid(null, null)).isTrue();
        assertThat(pdfOpcional.isValid(vazio, null)).isTrue();
    }

    @Test
    void nuloEVazioSaoInvalidosNosValidadoresObrigatorios() {
        MultipartFile vazio = new MockMultipartFile("arquivo", "capa.png", "image/png", new byte[0]);

        assertThat(imagemObrigatoria.isValid(null, null)).isFalse();
        assertThat(imagemObrigatoria.isValid(vazio, null)).isFalse();
        assertThat(pdfObrigatorio.isValid(null, null)).isFalse();
        assertThat(pdfObrigatorio.isValid(vazio, null)).isFalse();
    }

    // ---------- imagem ----------

    @ParameterizedTest
    @CsvSource({
            "capa.png,image/png",
            "capa.jpg,image/jpeg",
            "capa.jpeg,image/jpeg",
            "capa.webp,image/webp",
            "CAPA.PNG,IMAGE/PNG",
            "foto.Jpg,Image/Jpeg"
    })
    void imagemValidaPassaNasDuasVersoes(String nome, String tipo) {
        MultipartFile file = arquivo(nome, tipo);

        assertThat(imagemOpcional.isValid(file, null)).isTrue();
        assertThat(imagemObrigatoria.isValid(file, null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "capa.gif,image/gif",
            "capa.png,application/pdf",
            "capa.pdf,image/png",
            "capa.png,text/plain",
            "capa,image/png",
            "capa.png.exe,image/png"
    })
    void imagemComExtensaoOuMimeInvalidoFalhaNasDuasVersoes(String nome, String tipo) {
        MultipartFile file = arquivo(nome, tipo);

        assertThat(imagemOpcional.isValid(file, null)).isFalse();
        assertThat(imagemObrigatoria.isValid(file, null)).isFalse();
    }

    @Test
    void imagemSemNomeOuSemTipoDeConteudoFalha() {
        MultipartFile semNome = mock(MultipartFile.class);
        when(semNome.isEmpty()).thenReturn(false);
        when(semNome.getOriginalFilename()).thenReturn(null);
        when(semNome.getContentType()).thenReturn("image/png");

        MultipartFile semTipo = mock(MultipartFile.class);
        when(semTipo.isEmpty()).thenReturn(false);
        when(semTipo.getOriginalFilename()).thenReturn("capa.png");
        when(semTipo.getContentType()).thenReturn(null);

        assertThat(imagemOpcional.isValid(semNome, null)).isFalse();
        assertThat(imagemOpcional.isValid(semTipo, null)).isFalse();
        assertThat(imagemObrigatoria.isValid(semNome, null)).isFalse();
        assertThat(imagemObrigatoria.isValid(semTipo, null)).isFalse();
    }

    // ---------- pdf ----------

    @ParameterizedTest
    @CsvSource({
            "livro.pdf,application/pdf",
            "LIVRO.PDF,APPLICATION/PDF",
            "livro.pdf,application/x-pdf"
    })
    void pdfValidoPassaNasDuasVersoes(String nome, String tipo) {
        MultipartFile file = arquivo(nome, tipo);

        assertThat(pdfOpcional.isValid(file, null)).isTrue();
        assertThat(pdfObrigatorio.isValid(file, null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "livro.docx,application/pdf",
            "livro.pdf,image/png",
            "livro.pdf,text/plain",
            "livro,application/pdf",
            "livro.pdf.exe,application/pdf"
    })
    void pdfComExtensaoOuMimeInvalidoFalhaNasDuasVersoes(String nome, String tipo) {
        MultipartFile file = arquivo(nome, tipo);

        assertThat(pdfOpcional.isValid(file, null)).isFalse();
        assertThat(pdfObrigatorio.isValid(file, null)).isFalse();
    }

    @Test
    void pdfSemNomeOuSemTipoDeConteudoFalha() {
        MultipartFile semNome = mock(MultipartFile.class);
        when(semNome.isEmpty()).thenReturn(false);
        when(semNome.getOriginalFilename()).thenReturn(null);
        when(semNome.getContentType()).thenReturn("application/pdf");

        MultipartFile semTipo = mock(MultipartFile.class);
        when(semTipo.isEmpty()).thenReturn(false);
        when(semTipo.getOriginalFilename()).thenReturn("livro.pdf");
        when(semTipo.getContentType()).thenReturn(null);

        assertThat(pdfOpcional.isValid(semNome, null)).isFalse();
        assertThat(pdfOpcional.isValid(semTipo, null)).isFalse();
        assertThat(pdfObrigatorio.isValid(semNome, null)).isFalse();
        assertThat(pdfObrigatorio.isValid(semTipo, null)).isFalse();
    }
}
