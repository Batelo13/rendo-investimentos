package com.curso.gestaoinvestimentos.security;

/** Login JWT barrado pelo LimiteTentativasLogin: vira 429 no GlobalExceptionHandler. */
public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas de login. Tente novamente em 15 minutos.");
    }
}
