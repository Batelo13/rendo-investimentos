package com.curso.gestaoinvestimentos.controller;

import com.curso.gestaoinvestimentos.dto.LoginRequestDTO;
import com.curso.gestaoinvestimentos.dto.TokenResponseDTO;
import com.curso.gestaoinvestimentos.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Aula 13: login por token (API). O navegador continua no login por formulario. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record UsuarioLogadoDTO(String email, List<String> papeis) {
    }

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public TokenResponseDTO login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        return authService.login(dto, request.getRemoteAddr());
    }

    @GetMapping("/me")
    public UsuarioLogadoDTO me(Authentication autenticacao) {
        return new UsuarioLogadoDTO(autenticacao.getName(),
                autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                        .filter(a -> a.startsWith("ROLE_")).sorted().toList());
    }
}
