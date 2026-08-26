package com.curso.gestaoinvestimentos.dto;

public record CadastroResponseDTO(
        String message,
        boolean emailVerificationRequired
) {
}
