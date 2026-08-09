package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.TipoOperacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OperacaoRequestDTO(

        @NotNull(message = "Acao e obrigatoria")
        Long acaoId,

        @NotNull(message = "Corretora e obrigatoria")
        Long corretoraId,

        @NotNull(message = "Tipo e obrigatorio (COMPRA ou VENDA)")
        TipoOperacao tipo,

        @NotNull(message = "Quantidade e obrigatoria")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "Preco unitario e obrigatorio")
        @Positive(message = "Preco unitario deve ser maior que zero")
        BigDecimal precoUnitario
) {
}
