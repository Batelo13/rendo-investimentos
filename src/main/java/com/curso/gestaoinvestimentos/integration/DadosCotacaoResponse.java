package com.curso.gestaoinvestimentos.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DadosCotacaoResponse(
        String ticker,
        String nomeEmpresa,
        String moeda,
        BigDecimal cotacaoAtual,
        LocalDateTime dataHoraCotacao,
        String logoUrl
) {
}
