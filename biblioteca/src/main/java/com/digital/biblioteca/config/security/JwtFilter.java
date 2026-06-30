package com.digital.biblioteca.config.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.digital.biblioteca.service.implement.UsuarioDetailsServiceImpl;
import com.digital.biblioteca.treatment.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UsuarioDetailsServiceImpl usuarioDetailsService;
    private final ObjectMapper objectMapper;
    private final CookieUtil cookieUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Optional<String> tokenOpt = extrairToken(request);

        if (tokenOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String matricula = tokenService.validateToken(tokenOpt.get());

            UserDetails user = usuarioDetailsService.loadUserByUsername(matricula);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JWTVerificationException ex) {

            escreverErro(request, response, HttpStatus.UNAUTHORIZED,
                    "Token inválido ou expirado", ex.getMessage());
            return;

        } catch (UsernameNotFoundException ex) {

            escreverErro(request, response, HttpStatus.UNAUTHORIZED,
                    "Usuário do token não existe mais", ex.getMessage());
            return;

        }

        filterChain.doFilter(request, response);

    }

    private Optional<String> extrairToken(HttpServletRequest request) {

        Optional<String> doCookie = cookieUtil.readCookie(request, CookieUtil.ACCESS_COOKIE);
        if (doCookie.isPresent()) {
            return doCookie;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return Optional.of(header.substring(7));
        }

        return Optional.empty();
    }

    private void escreverErro(HttpServletRequest request,
                              HttpServletResponse response,
                              HttpStatus status,
                              String mensagem,
                              String detalhes) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError erro = ApiError.of(status, mensagem, detalhes, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), erro);

    }
}
