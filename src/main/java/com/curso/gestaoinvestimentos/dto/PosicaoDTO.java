package com.curso.gestaoinvestimentos.dto;

import java.math.BigDecimal;

public record PosicaoDTO(
        String acaoTicker,
        String corretoraNome,
        BigDecimal quantidade,
        BigDecimal precoMedio,
        BigDecimal valorInvestido,
        BigDecimal valorAtual
) {
}
