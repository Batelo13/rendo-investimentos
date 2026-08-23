package com.curso.gestaoinvestimentos.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Quando o login social nao encontra uma conta Rendo com o email do provedor,
 * manda o usuario pro cadastro ja com nome/email preenchidos (login.js le
 * esses parametros e completa o formulario) -- em vez de so um erro generico.
 * Nenhuma conta e criada aqui: o usuario ainda precisa informar CPF e senha
 * pra concluir o cadastro normalmente.
 */
@Component
public class LoginSocialFailureHandler implements AuthenticationFailureHandler {

    private final AuthenticationFailureHandler generico = new SimpleUrlAuthenticationFailureHandler("/login?error");

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof ContaOAuthNaoEncontradaException semConta) {
            String destino = UriComponentsBuilder.fromPath("/login")
                    .queryParam("criarConta", "1")
                    .queryParam("nome", semConta.getNome() == null ? "" : semConta.getNome())
                    .queryParam("email", semConta.getEmail())
                    .build()
                    .encode()
                    .toUriString();
            response.sendRedirect(destino);
            return;
        }
        generico.onAuthenticationFailure(request, response, exception);
    }
}
