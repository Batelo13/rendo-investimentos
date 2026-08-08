package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AcaoRequestDTO(

        @NotBlank(message = "Ticker e obrigatorio")
        @Pattern(regexp = "[A-Za-z0-9.]{1,10}", message = "Ticker invalido")
        String ticker,

        @NotNull(message = "Mercado e obrigatorio (BRASIL ou EUA)")
        Mercado mercado
) {
}
