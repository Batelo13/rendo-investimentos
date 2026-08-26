package com.curso.gestaoinvestimentos.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Diferencia "email nao verificado" de qualquer outra falha de login por
 * formulario (senha errada, conta bloqueada por admin) -- mesmo padrao ja
 * usado por LoginSocialFailureHandler pro login social.
 *
 * EmailNaoVerificadoException e lancada dentro de UsuarioDetailsService
 * .loadUserByUsername -- DaoAuthenticationProvider.retrieveUser() embrulha
 * qualquer excecao lancada ali (exceto UsernameNotFoundException) numa
 * InternalAuthenticationServiceException, preservando a original como cause.
 * Por isso a checagem abaixo olha a cause, nao o tipo direto.
 */
@Component
public class LoginFormFailureHandler implements AuthenticationFailureHandler {

    private final AuthenticationFailureHandler generico = new SimpleUrlAuthenticationFailureHandler("/login?error");
    private final AuthenticationFailureHandler emailNaoVerificado =
            new SimpleUrlAuthenticationFailureHandler("/login?erro=email-nao-verificado");

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException, ServletException {
        boolean emailNaoVerificadoLancado = exception instanceof EmailNaoVerificadoException
                || (exception instanceof InternalAuthenticationServiceException
                    && exception.getCause() instanceof EmailNaoVerificadoException);

        if (emailNaoVerificadoLancado) {
            emailNaoVerificado.onAuthenticationFailure(request, response, exception);
            return;
        }
        generico.onAuthenticationFailure(request, response, exception);
    }
}
