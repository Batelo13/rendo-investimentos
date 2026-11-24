package com.curso.gestaoinvestimentos.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank(message = "Email deve ser informado") String email,
        @NotBlank(message = "Senha deve ser informada") String senha
) {
}
