package com.digital.biblioteca.config.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static final String ACCESS_COOKIE = "smartteca_access";
    public static final String REFRESH_COOKIE = "smartteca_refresh";
    public static final Duration ACCESS_DURATION = Duration.ofHours(3);
    public static final Duration REFRESH_DURATION = Duration.ofDays(7);

    private final CookieProperties properties;

    public void setAccessCookie(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", construirCookie(ACCESS_COOKIE, token, ACCESS_DURATION, "/").toString());
    }

    public void setRefreshCookie(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", construirCookie(REFRESH_COOKIE, token, REFRESH_DURATION, "/api/v1/auth").toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", construirCookie(ACCESS_COOKIE, "", Duration.ZERO, "/").toString());
        response.addHeader("Set-Cookie", construirCookie(REFRESH_COOKIE, "", Duration.ZERO, "/api/v1/auth").toString());
    }

    public Optional<String> readCookie(HttpServletRequest request, String nome) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (Cookie cookie : request.getCookies()) {
            if (nome.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private ResponseCookie construirCookie(String nome, String valor, Duration maxAge, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite(properties.getSameSite())
                .path(path)
                .maxAge(maxAge);

        if (properties.getDomain() != null && !properties.getDomain().isBlank()) {
            builder.domain(properties.getDomain());
        }

        return builder.build();
    }
}
