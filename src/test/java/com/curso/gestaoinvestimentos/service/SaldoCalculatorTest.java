package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaldoCalculatorTest {

    private Operacao operacao(TipoOperacao tipo, String quantidade, String precoUnitario) {
        return operacao(tipo, quantidade, precoUnitario, "1");
    }

    private Operacao operacao(TipoOperacao tipo, String quantidade, String precoUnitario, String taxaCambio) {
        Operacao operacao = new Operacao();
        operacao.setTipo(tipo);
        operacao.setQuantidade(new BigDecimal(quantidade));
        operacao.setPrecoUnitario(new BigDecimal(precoUnitario));
        operacao.setTaxaCambio(new BigDecimal(taxaCambio));
        return operacao;
    }

    @Test
    void semOperacoesSaldoEIgualAoInicial() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of());

        assertEquals(0, saldo.compareTo(new BigDecimal("100000.00")));
    }

    @Test
    void compraDescontaDoSaldo() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00")
        ));

        assertEquals(0, saldo.compareTo(new BigDecimal("99000.00")));
    }

    @Test
    void vendaSomaAoSaldo() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00"),
                operacao(TipoOperacao.VENDA, "5", "150.00")
        ));

        // 100000 - (10*100) + (5*150) = 100000 - 1000 + 750 = 99750
        assertEquals(0, saldo.compareTo(new BigDecimal("99750.00")));
    }

    @Test
    void compraEmAcaoEuaDescontaValorConvertidoPelaTaxaDeCambio() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00", "5.00")
        ));

        // 100000 - (10*100*5) = 100000 - 5000 = 95000
        assertEquals(0, saldo.compareTo(new BigDecimal("95000.00")));
    }

    @Test
    void vendaEmAcaoEuaSomaValorConvertidoPelaTaxaDeCambio() {
        BigDecimal saldo = SaldoCalculator.calcular(new BigDecimal("100000.00"), List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00", "5.00"),
                operacao(TipoOperacao.VENDA, "10", "120.00", "5.50")
        ));

        // 100000 - (10*100*5) + (10*120*5.5) = 100000 - 5000 + 6600 = 101600
        assertEquals(0, saldo.compareTo(new BigDecimal("101600.00")));
    }
}
