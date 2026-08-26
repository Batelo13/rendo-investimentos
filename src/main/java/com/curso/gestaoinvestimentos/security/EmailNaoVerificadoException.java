package com.curso.gestaoinvestimentos.security;

import org.springframework.security.authentication.DisabledException;

/**
 * Lancada no lugar de DisabledException quando o motivo especifico do
 * bloqueio de login e email nao verificado (em vez de conta desativada por
 * um admin) -- permite ao LoginFormFailureHandler diferenciar os dois casos,
 * do mesmo jeito que ContaOAuthNaoEncontradaException ja diferencia motivos
 * de falha no login social.
 */
public class EmailNaoVerificadoException extends DisabledException {

    public EmailNaoVerificadoException(String message) {
        super(message);
    }
}
