package com.digital.smartteca.gateway;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;

/** Par RSA de teste e emissao de tokens com o mesmo formato do core-service (issuer, subject, claim "roles"). */
public final class TestKeys {

    public final KeyPair pair;

    public TestKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.pair = generator.generateKeyPair();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public String publicPem() {
        return pem("PUBLIC KEY", pair.getPublic().getEncoded());
    }

    public String privatePem() {
        return pem("PRIVATE KEY", pair.getPrivate().getEncoded());
    }

    public String accessToken(String matricula, String roles) {
        return accessToken(matricula, roles, Instant.now().plusSeconds(3600));
    }

    public String accessToken(String matricula, String roles, Instant expira) {
        return token("smartteca-access", matricula, roles, expira, pair);
    }

    public String refreshToken(String matricula) {
        return token("smartteca-refresh", matricula, null, Instant.now().plusSeconds(3600), pair);
    }

    public static String token(String issuer, String matricula, String roles, Instant expira, KeyPair keys) {
        var builder = JWT.create().withIssuer(issuer).withSubject(matricula).withExpiresAt(expira);
        if (roles != null) {
            builder.withClaim("roles", roles);
        }
        return builder.sign(Algorithm.RSA256((RSAPublicKey) keys.getPublic(), (RSAPrivateKey) keys.getPrivate()));
    }

    public String tokenAssinadoPorOutraChave(String matricula, String roles) {
        return token("smartteca-access", matricula, roles, Instant.now().plusSeconds(3600), outroPar());
    }

    public static KeyPair outroPar() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String pem(String tipo, byte[] der) {
        return "-----BEGIN " + tipo + "-----" + Base64.getEncoder().encodeToString(der) + "-----END " + tipo + "-----";
    }
}
