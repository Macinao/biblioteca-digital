package com.digital.biblioteca.controller;

import com.digital.biblioteca.model.Usuario;
import com.digital.biblioteca.request.CadastrarUsuarioRequest;
import com.digital.biblioteca.response.UsuarioResponse;
import com.digital.biblioteca.service.implement.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthServiceImpl authService;

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(@AuthenticationPrincipal Usuario usuario) {

        return ResponseEntity.status(HttpStatus.OK)
                .body(new UsuarioResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getMatricula(),
                        usuario.getEmail(),
                        usuario.getTelefone(),
                        usuario.getRole()
                ));

    }

    @PostMapping
    @PreAuthorize("hasRole('BIBLIOTECARIO')")
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid CadastrarUsuarioRequest request) {

        authService.cadastro(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

}
