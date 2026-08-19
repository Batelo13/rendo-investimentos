package com.curso.gestaoinvestimentos.service;

import com.curso.gestaoinvestimentos.model.Operacao;
import com.curso.gestaoinvestimentos.model.TipoOperacao;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calcula o saldo virtual disponivel a partir do saldo inicial da carteira e
 * do historico de operacoes em ordem cronologica. Mesmo raciocinio do
 * PosicaoCalculator: Operacao e a fonte da verdade, saldo e um valor
 * derivado, nunca guardado separadamente -- cancelar uma compra "devolve" o
 * saldo automaticamente, so por ela sair do historico ATIVA usado aqui.
 *
 * taxaCambio e sempre 1 pra acoes BRASIL e a taxa USD->BRL vigente no
 * momento de cada operacao pra acoes EUA (gravada na propria Operacao,
 * nunca recalculada com a taxa "de agora" -- mesma garantia de historico
 * imutavel/deterministico ja usada pro preco).
 */
public class SaldoCalculator {

    public static BigDecimal calcular(BigDecimal saldoInicial, List<Operacao> operacoesEmOrdemCronologica) {
        BigDecimal saldo = saldoInicial;

        for (Operacao operacao : operacoesEmOrdemCronologica) {
            BigDecimal valor = operacao.getPrecoUnitario()
                    .multiply(operacao.getQuantidade())
                    .multiply(operacao.getTaxaCambio());
            if (operacao.getTipo() == TipoOperacao.COMPRA) {
                saldo = saldo.subtract(valor);
            } else {
                saldo = saldo.add(valor);
            }
        }

        return saldo;
    }
}
