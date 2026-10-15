package com.digital.smartteca.common.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsaKeyLoaderTest {

    static KeyPair pair;

    @BeforeAll
    static void gerarPar() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        pair = generator.generateKeyPair();
    }

    @Test
    void carregaChavesEmPemComQuebrasDeLinhaEAssinaEVerifica() throws Exception {
        RSAPrivateKey privada = RsaKeyLoader.loadPrivateKey(pem("PRIVATE KEY", pair.getPrivate().getEncoded(), true));
        RSAPublicKey publica = RsaKeyLoader.loadPublicKey(pem("PUBLIC KEY", pair.getPublic().getEncoded(), true));

        assertTrue(verifica(privada, publica));
    }

    @Test
    void carregaChavesEmPemDeUmaLinhaSoComoVemDeVariavelDeAmbiente() throws Exception {
        RSAPrivateKey privada = RsaKeyLoader.loadPrivateKey(pem("PRIVATE KEY", pair.getPrivate().getEncoded(), false));
        RSAPublicKey publica = RsaKeyLoader.loadPublicKey(pem("PUBLIC KEY", pair.getPublic().getEncoded(), false));

        assertTrue(verifica(privada, publica));
    }

    @Test
    void chaveCarregadaTemOMesmoConteudoDaOriginal() throws Exception {
        RSAPublicKey publica = RsaKeyLoader.loadPublicKey(pem("PUBLIC KEY", pair.getPublic().getEncoded(), true));

        assertArrayEquals(pair.getPublic().getEncoded(), publica.getEncoded());
    }

    @Test
    void conteudoQueNaoEBase64ValidoFalha() {
        assertThrows(IllegalArgumentException.class,
                () -> RsaKeyLoader.loadPublicKey("-----BEGIN PUBLIC KEY-----%%%-----END PUBLIC KEY-----"));
    }

    @Test
    void base64ValidoMasQueNaoEUmaChaveFalha() {
        String lixo = "-----BEGIN PUBLIC KEY-----" + Base64.getEncoder().encodeToString("nao sou uma chave".getBytes())
                + "-----END PUBLIC KEY-----";

        assertThrows(Exception.class, () -> RsaKeyLoader.loadPublicKey(lixo));
    }

    @Test
    void pemDeChavePublicaNaoCarregaComoPrivada() {
        String pemPublico = pem("PUBLIC KEY", pair.getPublic().getEncoded(), true);

        assertThrows(Exception.class, () -> RsaKeyLoader.loadPrivateKey(pemPublico));
    }

    private static boolean verifica(RSAPrivateKey privada, RSAPublicKey publica) throws Exception {
        byte[] dados = "matricula|ROLE_BIBLIOTECARIO".getBytes();
        Signature assinador = Signature.getInstance("SHA256withRSA");
        assinador.initSign(privada);
        assinador.update(dados);
        byte[] assinatura = assinador.sign();

        Signature verificador = Signature.getInstance("SHA256withRSA");
        verificador.initVerify(publica);
        verificador.update(dados);
        return verificador.verify(assinatura);
    }

    private static String pem(String tipo, byte[] der, boolean comQuebras) {
        String base64 = comQuebras
                ? Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der)
                : Base64.getEncoder().encodeToString(der);
        String separador = comQuebras ? "\n" : "";
        return "-----BEGIN " + tipo + "-----" + separador + base64 + separador + "-----END " + tipo + "-----";
    }
}
