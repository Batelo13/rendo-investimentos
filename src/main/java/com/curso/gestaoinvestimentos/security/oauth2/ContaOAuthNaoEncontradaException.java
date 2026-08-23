package com.curso.gestaoinvestimentos.security.oauth2;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

/**
 * Login social com email que ainda nao tem conta no Rendo. Carrega nome/email
 * do provedor pra que o LoginSocialFailureHandler consiga pre-preencher o
 * formulario de cadastro em vez de so mostrar um erro generico -- CPF e senha
 * continuam obrigatorios e sendo digitados pelo usuario, nenhuma conta e
 * criada automaticamente.
 */
public class ContaOAuthNaoEncontradaException extends OAuth2AuthenticationException {

    private final String nome;
    private final String email;

    public ContaOAuthNaoEncontradaException(String nome, String email) {
        super(new OAuth2Error("conta_nao_encontrada"), "Nenhuma conta Rendo encontrada para " + email);
        this.nome = nome;
        this.email = email;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }
}
