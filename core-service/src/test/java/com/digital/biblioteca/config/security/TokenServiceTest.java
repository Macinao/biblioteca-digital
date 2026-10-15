package com.digital.biblioteca.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.digital.biblioteca.enums.Role;
import com.digital.biblioteca.model.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    static KeyPair pair;
    static KeyPair outroPar;

    TokenService service;
    Usuario usuario;

    @BeforeAll
    static void gerarChaves() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        pair = generator.generateKeyPair();
        outroPar = generator.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        service = new TokenService();
        ReflectionTestUtils.setField(service, "privateKeyPem", pem("PRIVATE KEY", pair.getPrivate().getEncoded()));
        ReflectionTestUtils.setField(service, "publicKeyPem", pem("PUBLIC KEY", pair.getPublic().getEncoded()));
        ReflectionTestUtils.invokeMethod(service, "init");

        usuario = new Usuario();
        usuario.setMatricula("2023001");
        usuario.setRole(Role.BIBLIOTECARIO);
    }

    @Test
    void accessTokenCarregaIssuerSubjectERole() {
        DecodedJWT jwt = JWT.decode(service.generateAccessToken(usuario));

        assertThat(jwt.getIssuer()).isEqualTo("smartteca-access");
        assertThat(jwt.getSubject()).isEqualTo("2023001");
        assertThat(jwt.getClaim("roles").asString()).isEqualTo("BIBLIOTECARIO");
    }

    @Test
    void accessTokenExpiraEmTresHoras() {
        DecodedJWT jwt = JWT.decode(service.generateAccessToken(usuario));

        Duration duracao = Duration.between(jwt.getIssuedAtAsInstant(), jwt.getExpiresAtAsInstant());
        assertThat(duracao).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void refreshTokenTemIssuerProprioENaoCarregaRole() {
        DecodedJWT jwt = JWT.decode(service.generateRefreshToken(usuario));

        assertThat(jwt.getIssuer()).isEqualTo("smartteca-refresh");
        assertThat(jwt.getSubject()).isEqualTo("2023001");
        assertThat(jwt.getClaim("roles").isMissing()).isTrue();
    }

    @Test
    void refreshTokenExpiraEmSeteDias() {
        DecodedJWT jwt = JWT.decode(service.generateRefreshToken(usuario));

        Duration duracao = Duration.between(jwt.getIssuedAtAsInstant(), jwt.getExpiresAtAsInstant());
        assertThat(duracao).isEqualTo(Duration.ofDays(7));
    }

    @Test
    void validateRefreshTokenDevolveAMatricula() {
        String token = service.generateRefreshToken(usuario);

        assertThat(service.validateRefreshToken(token)).isEqualTo("2023001");
    }

    @Test
    void accessTokenNaoEAceitoComoRefresh() {
        String access = service.generateAccessToken(usuario);

        assertThatThrownBy(() -> service.validateRefreshToken(access)).isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void refreshTokenExpiradoERejeitado() {
        String expirado = tokenRefreshCom(pair, Instant.now().minusSeconds(60));

        assertThatThrownBy(() -> service.validateRefreshToken(expirado)).isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void tokenAssinadoPorOutraChaveERejeitado() {
        String forjado = tokenRefreshCom(outroPar, Instant.now().plusSeconds(3600));

        assertThatThrownBy(() -> service.validateRefreshToken(forjado)).isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void tokenAdulteradoERejeitado() {
        String token = service.generateRefreshToken(usuario);
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> service.validateRefreshToken(adulterado)).isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void lixoQueNaoEJwtERejeitado() {
        assertThatThrownBy(() -> service.validateRefreshToken("isto-nao-e-um-jwt")).isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void chavePrivadaInvalidaFazOBeanFalharNaInicializacao() {
        TokenService quebrado = new TokenService();
        ReflectionTestUtils.setField(quebrado, "privateKeyPem", "-----BEGIN PRIVATE KEY-----lixo-----END PRIVATE KEY-----");
        ReflectionTestUtils.setField(quebrado, "publicKeyPem", pem("PUBLIC KEY", pair.getPublic().getEncoded()));

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(quebrado, "init")).isInstanceOf(Exception.class);
    }

    private static String tokenRefreshCom(KeyPair keys, Instant expira) {
        return JWT.create()
                .withIssuer("smartteca-refresh")
                .withSubject("2023001")
                .withExpiresAt(expira)
                .sign(Algorithm.RSA256((RSAPublicKey) keys.getPublic(), (RSAPrivateKey) keys.getPrivate()));
    }

    private static String pem(String tipo, byte[] der) {
        return "-----BEGIN " + tipo + "-----" + Base64.getEncoder().encodeToString(der) + "-----END " + tipo + "-----";
    }
}
