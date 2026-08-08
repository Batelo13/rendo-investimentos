package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.Mercado;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcaoResponseDTO(
        Long id,
        String ticker,
        String nomeEmpresa,
        Mercado mercado,
        String moeda,
        BigDecimal cotacaoAtual,
        LocalDateTime dataHoraCotacao
) {
}
