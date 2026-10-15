package com.digital.smartteca.common.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Converte uma chave RSA em formato PEM (texto, com os cabecalhos
 * "-----BEGIN ...-----") numa chave Java utilizavel pelo java-jwt.
 *
 * Usado tanto pelo core-service (que tem a chave privada, para assinar
 * tokens) quanto por qualquer servico que so precise verificar tokens
 * (que recebe so a chave publica, sem poder de assinar).
 */
public final class RsaKeyLoader {

    private RsaKeyLoader() {
    }

    public static RSAPublicKey loadPublicKey(String pem) throws Exception {
        byte[] bytes = decode(pem, "PUBLIC KEY");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static RSAPrivateKey loadPrivateKey(String pem) throws Exception {
        byte[] bytes = decode(pem, "PRIVATE KEY");
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private static byte[] decode(String pem, String label) {
        String clean = pem
                .replace("-----BEGIN " + label + "-----", "")
                .replace("-----END " + label + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(clean);
    }
}
