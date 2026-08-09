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
 * que ja nao existe mais.
 */
public class PosicaoCalculator {

    public record Posicao(int quantidade, BigDecimal precoMedio, int quantidadeMinimaHistorica) {
    }

    public static Posicao calcular(List<Operacao> operacoesEmOrdemCronologica) {
        int quantidade = 0;
        BigDecimal precoMedio = BigDecimal.ZERO;
        int quantidadeMinima = 0;

        for (Operacao operacao : operacoesEmOrdemCronologica) {
            if (operacao.getTipo() == TipoOperacao.COMPRA) {
                int novaQuantidade = quantidade + operacao.getQuantidade();
                BigDecimal custoAntigo = precoMedio.multiply(BigDecimal.valueOf(quantidade));
                BigDecimal custoNovo = operacao.getPrecoUnitario().multiply(BigDecimal.valueOf(operacao.getQuantidade()));
                precoMedio = custoAntigo.add(custoNovo)
                        .divide(BigDecimal.valueOf(novaQuantidade), 6, RoundingMode.HALF_UP);
                quantidade = novaQuantidade;
            } else {
                quantidade -= operacao.getQuantidade();
                if (quantidade == 0) {
                    precoMedio = BigDecimal.ZERO;
                }
            }
            quantidadeMinima = Math.min(quantidadeMinima, quantidade);
        }

        return new Posicao(quantidade, precoMedio, quantidadeMinima);
    }
}
