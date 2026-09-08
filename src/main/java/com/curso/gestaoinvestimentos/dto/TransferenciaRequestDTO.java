package com.curso.gestaoinvestimentos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransferenciaRequestDTO(

        @NotNull(message = "Acao e obrigatoria")
        Long acaoId,

        @NotNull(message = "Corretora de origem e obrigatoria")
        Long corretoraOrigemId,

        @NotNull(message = "Corretora de destino e obrigatoria")
        Long corretoraDestinoId,

        @NotNull(message = "Quantidade e obrigatoria")
        @Positive(message = "Quantidade deve ser maior que zero")
        BigDecimal quantidade
) {
}
