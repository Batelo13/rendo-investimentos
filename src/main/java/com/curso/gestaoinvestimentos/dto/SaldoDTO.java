package com.curso.gestaoinvestimentos.dto;

import java.math.BigDecimal;

public record SaldoDTO(
        BigDecimal saldoInicial,
        BigDecimal saldoDisponivel
) {
}
