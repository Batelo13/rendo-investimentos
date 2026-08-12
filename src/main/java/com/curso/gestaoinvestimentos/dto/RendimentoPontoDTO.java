package com.curso.gestaoinvestimentos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RendimentoPontoDTO(
        LocalDateTime timestamp,
        BigDecimal rendimento
) {
}
