package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calcula a posicao (quantidade, preco medio) a partir de um historico de
 * operacoes em ordem cronologica. Preco medio so muda em COMPRA (media
 * ponderada); VENDA reduz quantidade sem alterar o preco medio de quem fica
 * na carteira, e reseta o preco medio quando a quantidade zera -- sem isso,
 * uma posicao que foi zerada e recomecada "herdaria" preco medio de um lote
 * que ja nao existe mais. Quantidade e BigDecimal para suportar acoes
 * fracionarias.
 */
public class PosicaoCalculator {

    public record Posicao(BigDecimal quantidade, BigDecimal precoMedio, BigDecimal quantidadeMinimaHistorica) {
    }

    public static Posicao calcular(List<Operacao> operacoesEmOrdemCronologica) {
        BigDecimal quantidade = BigDecimal.ZERO;
        BigDecimal precoMedio = BigDecimal.ZERO;
        BigDecimal quantidadeMinima = BigDecimal.ZERO;

        for (Operacao operacao : operacoesEmOrdemCronologica) {
            if (operacao.getTipo() == TipoOperacao.COMPRA) {
                BigDecimal novaQuantidade = quantidade.add(operacao.getQuantidade());
                if (novaQuantidade.compareTo(BigDecimal.ZERO) == 0) {
                    precoMedio = BigDecimal.ZERO;
                } else {
                    BigDecimal custoAntigo = precoMedio.multiply(quantidade);
                    BigDecimal custoNovo = operacao.getPrecoUnitario().multiply(operacao.getQuantidade());
                    precoMedio = custoAntigo.add(custoNovo)
                            .divide(novaQuantidade, 6, RoundingMode.HALF_UP);
                }
                quantidade = novaQuantidade;
            } else {
                quantidade = quantidade.subtract(operacao.getQuantidade());
                if (quantidade.compareTo(BigDecimal.ZERO) == 0) {
                    precoMedio = BigDecimal.ZERO;
                }
            }
            quantidadeMinima = quantidadeMinima.min(quantidade);
        }

        return new Posicao(quantidade, precoMedio, quantidadeMinima);
    }
}
