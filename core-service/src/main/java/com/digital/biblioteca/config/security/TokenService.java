package com.digital.biblioteca.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.digital.biblioteca.model.Usuario;
import com.digital.smartteca.common.security.RsaKeyLoader;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

@Service
public class TokenService {

    private static final String ISSUER_ACCESS = "smartteca-access";
    private static final String ISSUER_REFRESH = "smartteca-refresh";
    private static final long ACCESS_EXPIRES_SECONDS = 10800L;
    private static final long REFRESH_EXPIRES_SECONDS = 604800L;

    @Value("${spring.keys.jwt.private-key}")
    private String privateKeyPem;

    @Value("${spring.keys.jwt.public-key}")
    private String publicKeyPem;

    private Algorithm algorithm;

    // Faz o parse das chaves PEM uma vez, na inicializacao do bean, em vez de
    // repetir esse trabalho a cada emissao/validacao de token. O core-service
    // guarda as duas chaves porque ele e o unico que precisa assinar (chave
    // privada) e tambem valida internamente (chave publica) ate a validacao
    // ser centralizada no api-gateway, na Fase 6.
    @PostConstruct
    private void init() throws Exception {
        RSAPublicKey publicKey = RsaKeyLoader.loadPublicKey(publicKeyPem);
        RSAPrivateKey privateKey = RsaKeyLoader.loadPrivateKey(privateKeyPem);
        this.algorithm = Algorithm.RSA256(publicKey, privateKey);
    }

    public String generateAccessToken(Usuario usuario) {
        return JWT.create()
                .withIssuer(ISSUER_ACCESS)
                .withSubject(usuario.getMatricula())
                .withClaim("roles", usuario.getRole().toString())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(ACCESS_EXPIRES_SECONDS))
                .sign(algorithm);

    }

    public String generateRefreshToken(Usuario usuario) {
        return JWT.create()
                .withIssuer(ISSUER_REFRESH)
                .withSubject(usuario.getMatricula())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(REFRESH_EXPIRES_SECONDS))
                .sign(algorithm);

    }

    public String validateRefreshToken(String token) {

        return JWT.require(algorithm)
                .withIssuer(ISSUER_REFRESH)
                .build()
                .verify(token)
                .getSubject();

    }
}
