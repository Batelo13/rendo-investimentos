package com.curso.gestaoinvestimentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CorretoraRequestDTO(

        @NotBlank(message = "CNPJ e obrigatorio")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 digitos numericos, sem pontuacao")
        String cnpj
) {
}
