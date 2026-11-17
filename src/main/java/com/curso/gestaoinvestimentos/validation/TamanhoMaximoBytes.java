package com.curso.gestaoinvestimentos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

/**
 * Limite em bytes UTF-8 (nao em caracteres). Existe por causa do BCrypt, que so
 * aceita senhas de ate 72 bytes: sem esse limite na borda, uma senha longa (ou
 * curta com acentos) virava erro interno em vez de 400 (Modulo 11).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TamanhoMaximoBytes.Validador.class)
public @interface TamanhoMaximoBytes {

    int value();

    String message() default "Valor excede o tamanho maximo permitido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<TamanhoMaximoBytes, String> {

        private int maximo;

        @Override
        public void initialize(TamanhoMaximoBytes anotacao) {
            this.maximo = anotacao.value();
        }

        @Override
        public boolean isValid(String valor, ConstraintValidatorContext contexto) {
            return valor == null || valor.getBytes(StandardCharsets.UTF_8).length <= maximo;
        }
    }
}
