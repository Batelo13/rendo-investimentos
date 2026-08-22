package com.curso.gestaoinvestimentos.integration;

import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Supplier;

/**
 * Repete uma chamada a API externa quando a falha e transiente (timeout,
 * conexao recusada, erro 5xx do servidor) -- nunca em erro 4xx, que e
 * permanente (ex: CNPJ invalido nao vira valido tentando de novo).
 */
final class RetryExterno {

    private RetryExterno() {
    }

    static <T> T tentar(int tentativas, long atrasoMs, Supplier<T> acao) {
        RuntimeException ultimaFalha = null;
        for (int i = 0; i < tentativas; i++) {
            try {
                return acao.get();
            } catch (ResourceAccessException | HttpServerErrorException ex) {
                ultimaFalha = ex;
                if (i < tentativas - 1) {
                    dormir(atrasoMs);
                }
            }
        }
        throw ultimaFalha;
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
