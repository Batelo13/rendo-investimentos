package com.curso.gestaoinvestimentos.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gera CPFs sinteticos (nao pertencem a nenhuma pessoa real) com digito
 * verificador correto, um por chamada, para testes que precisam de um
 * Usuario persistido (cpf e unico no banco).
 */
public class CpfTestFixtures {

    private static final AtomicInteger CONTADOR = new AtomicInteger(100_000_000);

    private CpfTestFixtures() {
    }

    public static String proximoCpfValido() {
        return gerarCpfValido(CONTADOR.getAndIncrement());
    }

    private static String gerarCpfValido(int semente) {
        String base = String.format("%09d", semente % 1_000_000_000);
        int[] digitos = new int[11];
        for (int i = 0; i < 9; i++) {
            digitos[i] = base.charAt(i) - '0';
        }
        digitos[9] = calcularDigitoVerificador(digitos, 9);
        digitos[10] = calcularDigitoVerificador(digitos, 10);

        StringBuilder sb = new StringBuilder();
        for (int digito : digitos) {
            sb.append(digito);
        }
        return sb.toString();
    }

    private static int calcularDigitoVerificador(int[] digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += digitos[i] * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
