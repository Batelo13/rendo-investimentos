package com.curso.gestaoinvestimentos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificarEmailRequestDTO(

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @NotBlank(message = "Codigo e obrigatorio")
        @Pattern(regexp = "\\d{6}", message = "Codigo deve conter 6 digitos numericos")
        String codigo
) {
}
