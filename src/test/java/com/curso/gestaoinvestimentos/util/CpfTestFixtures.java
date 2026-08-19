package com.curso.gestaoinvestimentos.util;

import java.util.concurrent.atomic.AtomicInteger;
import com.curso.gestaoinvestimentos.validation.CpfValidator;

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
        digitos[9] = CpfValidator.calcularDigitoVerificador(digitos, 9);
        digitos[10] = CpfValidator.calcularDigitoVerificador(digitos, 10);

        StringBuilder sb = new StringBuilder();
        for (int digito : digitos) {
            sb.append(digito);
        }
        return sb.toString();
    }
}
