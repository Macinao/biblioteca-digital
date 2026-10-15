package com.digital.biblioteca.config.security;

import com.digital.biblioteca.service.implement.UsuarioDetailsServiceImpl;
import com.digital.biblioteca.treatment.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Substitui o antigo JwtFilter. Validar o JWT deixou de ser trabalho do
 * core-service — isso agora é responsabilidade exclusiva do api-gateway
 * (o único com a chave pública RSA). Esse filtro só confia no header
 * X-User-Matricula, que o gateway já garantiu ser legítimo (a rede
 * interna impede qualquer chamada direta ao core-service que não venha
 * por ele), e busca o Usuario completo no banco — o core-service é o
 * único serviço com acesso a essa tabela — para montar o principal que
 * @PreAuthorize e @AuthenticationPrincipal esperam.
 *
 * Buscar o usuário de novo a cada requisição, em vez de confiar cegamente
 * no papel que vier no header, mantém uma vantagem real: revogar ou
 * promover alguém tem efeito imediato aqui, sem esperar o token expirar.
 */
@Component
@RequiredArgsConstructor
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_MATRICULA = "X-User-Matricula";

    private final UsuarioDetailsServiceImpl usuarioDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String matricula = request.getHeader(HEADER_MATRICULA);

        if (matricula == null || matricula.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            UserDetails user = usuarioDetailsService.loadUserByUsername(matricula);

            // Desativar a conta vale já na próxima requisição, mesmo com o access token ainda dentro da validade.
            if (!user.isEnabled()) {
                escreverErro(request, response, HttpStatus.UNAUTHORIZED,
                        "Conta desativada", "Esta conta está desativada.");
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (UsernameNotFoundException ex) {

            escreverErro(request, response, HttpStatus.UNAUTHORIZED,
                    "Usuário do header não existe mais", ex.getMessage());
            return;

        }

        filterChain.doFilter(request, response);

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
