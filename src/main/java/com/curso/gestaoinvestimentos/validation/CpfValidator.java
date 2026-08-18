package com.curso.gestaoinvestimentos.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<CPF, String> {

    @Override
    public boolean isValid(String cpf, ConstraintValidatorContext context) {
        if (cpf == null || cpf.isBlank()) {
            return true;
        }
        if (!cpf.matches("\\d{11}") || todosOsDigitosIguais(cpf)) {
            return false;
        }

        int[] digitos = new int[11];
        for (int i = 0; i < 11; i++) {
            digitos[i] = cpf.charAt(i) - '0';
        }

        int dv1 = calcularDigitoVerificador(digitos, 9);
        int dv2 = calcularDigitoVerificador(digitos, 10);
        return digitos[9] == dv1 && digitos[10] == dv2;
    }

    private boolean todosOsDigitosIguais(String cpf) {
        return cpf.chars().distinct().count() == 1;
    }

    private int calcularDigitoVerificador(int[] digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += digitos[i] * (quantidade + 1 - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
