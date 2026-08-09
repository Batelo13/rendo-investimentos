package com.curso.gestaoinvestimentos.dto;

import java.math.BigDecimal;

public record PosicaoDTO(
        String acaoTicker,
        String corretoraNome,
        Integer quantidade,
        BigDecimal precoMedio,
        BigDecimal valorInvestido,
        BigDecimal valorAtual
) {
}
