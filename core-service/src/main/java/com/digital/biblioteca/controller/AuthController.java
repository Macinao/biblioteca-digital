package com.digital.biblioteca.controller;

import com.digital.biblioteca.config.security.CookieUtil;
import com.digital.biblioteca.request.LoginUsuarioRequest;
import com.digital.biblioteca.response.TokenAcessoResponse;
import com.digital.biblioteca.service.implement.AuthServiceImpl;
import com.digital.biblioteca.treatment.RegraNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthServiceImpl authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/login")
    public ResponseEntity<TokenAcessoResponse> login(@RequestBody @Valid LoginUsuarioRequest request,
                                                     HttpServletResponse response) {

        TokenAcessoResponse tokens = authService.login(request);

        cookieUtil.setAccessCookie(response, tokens.getTokenAcesso());
        cookieUtil.setRefreshCookie(response, tokens.getTokenRefresh());

        return ResponseEntity.status(HttpStatus.OK).body(tokens);

    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenAcessoResponse> refresh(HttpServletRequest request,
                                                       HttpServletResponse response) {

        String refreshToken = cookieUtil.readCookie(request, CookieUtil.REFRESH_COOKIE)
                .orElseThrow(() -> new RegraNegocioException("Refresh token ausente."));

        TokenAcessoResponse tokens = authService.refresh(refreshToken);

        cookieUtil.setAccessCookie(response, tokens.getTokenAcesso());
        cookieUtil.setRefreshCookie(response, tokens.getTokenRefresh());

        return ResponseEntity.status(HttpStatus.OK).body(tokens);

    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {

        cookieUtil.clearAuthCookies(response);

        return ResponseEntity.noContent().build();

    }

}
