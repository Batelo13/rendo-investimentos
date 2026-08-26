package com.curso.gestaoinvestimentos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ReenviarCodigoRequestDTO(

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email
) {
}
