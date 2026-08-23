package com.curso.gestaoinvestimentos.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cotacao de um indice de mercado (ex.: IBOVESPA) -- separado de
 * DadosCotacaoResponse porque este ultimo e o contrato usado pelo fluxo real
 * de compra/venda de acoes (nao mexer nele por causa de um widget novo).
 */
public record IndiceMercado(
        String nome,
        BigDecimal pontos,
        BigDecimal variacaoPercentual,
        LocalDateTime atualizadoEm
) {
}
