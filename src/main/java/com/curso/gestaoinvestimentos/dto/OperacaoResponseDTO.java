package com.curso.gestaoinvestimentos.dto;

import com.curso.gestaoinvestimentos.model.StatusOperacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OperacaoResponseDTO(
        Long id,
        TipoOperacao tipo,
        Integer quantidade,
        BigDecimal precoUnitario,
        LocalDateTime dataHora,
        StatusOperacao status,
        String acaoTicker,
        String corretoraNome,
        BigDecimal precoMedioNaVenda,
        BigDecimal lucroPrejuizoRealizado
) {
}
