package com.digital.biblioteca.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.digital.biblioteca.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class TokenService {

    private static final String ISSUER_ACCESS = "smartteca-access";
    private static final String ISSUER_REFRESH = "smartteca-refresh";
    private static final long ACCESS_EXPIRES_SECONDS = 10800L;
    private static final long REFRESH_EXPIRES_SECONDS = 604800L;

    @Value("${spring.keys.jwt.secret}")
    private String secret;

    private Algorithm getAlgorithm() {

        return Algorithm.HMAC256(secret);

    }

    public String generateAccessToken(Usuario usuario) {
        return JWT.create()
                .withIssuer(ISSUER_ACCESS)
                .withSubject(usuario.getMatricula())
                .withClaim("roles", usuario.getRole().toString())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(ACCESS_EXPIRES_SECONDS))
                .sign(getAlgorithm());

    }

    public String generateRefreshToken(Usuario usuario) {
        return JWT.create()
                .withIssuer(ISSUER_REFRESH)
                .withSubject(usuario.getMatricula())
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(REFRESH_EXPIRES_SECONDS))
                .sign(getAlgorithm());

    }

    public String validateToken(String token) {

        return JWT.require(getAlgorithm())
                .withIssuer(ISSUER_ACCESS)
                .build()
                .verify(token)
                .getSubject();

    }

    public String validateRefreshToken(String token) {

        return JWT.require(getAlgorithm())
                .withIssuer(ISSUER_REFRESH)
                .build()
                .verify(token)
                .getSubject();

    }
}
