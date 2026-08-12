package com.curso.gestaoinvestimentos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoCotacaoResponseDTO(
        BigDecimal preco,
        LocalDateTime capturadoEm
) {
}
