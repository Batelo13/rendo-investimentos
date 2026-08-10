package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PosicaoCalculatorTest {

    private Operacao operacao(TipoOperacao tipo, String quantidade, String precoUnitario) {
        Operacao operacao = new Operacao();
        operacao.setTipo(tipo);
        operacao.setQuantidade(new BigDecimal(quantidade));
        operacao.setPrecoUnitario(new BigDecimal(precoUnitario));
        return operacao;
    }

    @Test
    void compraUnicaDefinePrecoMedioIgualAoPrecoDeCompra() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(new BigDecimal("10")));
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void duasComprasCalculamMediaPonderada() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00"),
                operacao(TipoOperacao.COMPRA, "10", "200.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(new BigDecimal("20")));
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("150.00")));
    }

    @Test
    void vendaParcialNaoAlteraPrecoMedioDoQueSobrou() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00"),
                operacao(TipoOperacao.VENDA, "5", "50.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(new BigDecimal("5")));
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("100.00")));
    }

    @Test
    void zerarPosicaoEComprarDeNovoReiniciaPrecoMedioDoZero() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, "10", "10.00"),
                operacao(TipoOperacao.VENDA, "10", "10.00"),
                operacao(TipoOperacao.COMPRA, "5", "20.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(new BigDecimal("5")));
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("20.00")));
    }

    @Test
    void detectaSaldoNegativoHistoricoQuandoUmaCompraEhRemovidaDaSimulacao() {
        // Sem a compra (simulando um cancelamento), a venda de 5 ficaria a
        // descoberto naquele momento, mesmo que a compra seguinte "equilibre"
        // o total no final -- por isso o minimo historico, nao so o final,
        // e o que importa pra validar um cancelamento.
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.VENDA, "5", "50.00"),
                operacao(TipoOperacao.COMPRA, "10", "20.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(new BigDecimal("5")));
        assertEquals(0, resultado.quantidadeMinimaHistorica().compareTo(new BigDecimal("-5")));
    }

    @Test
    void naoQuebraQuandoQuantidadeVoltaAZeroPartindoDeNegativo() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, "10", "100.00"),
                operacao(TipoOperacao.VENDA, "15", "50.00"),
                operacao(TipoOperacao.COMPRA, "5", "20.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(BigDecimal.ZERO));
        assertEquals(0, resultado.precoMedio().compareTo(BigDecimal.ZERO));
        assertEquals(0, resultado.quantidadeMinimaHistorica().compareTo(new BigDecimal("-5")));
    }

    @Test
    void aceitaQuantidadeFracionaria() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, "0.5", "100.00"),
                operacao(TipoOperacao.COMPRA, "0.25", "200.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(new BigDecimal("0.75")));
        // (0.5*100 + 0.25*200) / 0.75 = 100 / 0.75 = 133.333333
        assertEquals(0, resultado.precoMedio().compareTo(new BigDecimal("133.333333")));
    }

    @Test
    void bloqueiaVendaFracionariaMaiorQueSaldoFracionario() {
        var resultado = PosicaoCalculator.calcular(List.of(
                operacao(TipoOperacao.COMPRA, "0.5", "100.00"),
                operacao(TipoOperacao.VENDA, "0.7", "100.00")
        ));

        assertEquals(0, resultado.quantidade().compareTo(new BigDecimal("-0.2")));
        assertEquals(true, resultado.quantidadeMinimaHistorica().compareTo(BigDecimal.ZERO) < 0);
    }
}
