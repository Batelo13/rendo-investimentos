package com.curso.gestaoinvestimentos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite de tentativas do login por formulario (Modulo 11, forca bruta).
 * <ul>
 *   <li>5 falhas do mesmo IP para o mesmo email bloqueiam esse par por 15 min;</li>
 *   <li>20 falhas do mesmo IP (quaisquer emails) bloqueiam o IP por 15 min.</li>
 * </ul>
 * Por IP+email, e nao so por email, para um atacante nao conseguir trancar a
 * conta do dono (que entra normalmente de outro IP). Login certo zera o par.
 * O IP e o remoteAddr: atras de proxy reverso viraria o IP do proxy.
 */
// ponytail: contador em memoria, por instancia; Redis/Bucket4j se houver mais de uma instancia.
@Component
public class LimiteTentativasLogin {

    static final int MAX_POR_EMAIL_E_IP = 5;
    static final int MAX_POR_IP = 20;
    static final Duration JANELA = Duration.ofMinutes(15);
    private static final int LIMPAR_ACIMA_DE = 10_000;
    private static final RequestMatcher POST_LOGIN =
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/login");

    private static final RedirectStrategy REDIRECT = new DefaultRedirectStrategy();

    private record Falhas(Instant inicio, int total) {}

    private final Map<String, Falhas> falhas = new ConcurrentHashMap<>();
    private final Clock relogio;

    public LimiteTentativasLogin() {
        this(Clock.systemUTC());
    }

    LimiteTentativasLogin(Clock relogio) {
        this.relogio = relogio;
    }

    public boolean bloqueado(String ip, String email) {
        return total(chave(ip, email)) >= MAX_POR_EMAIL_E_IP || total(ip) >= MAX_POR_IP;
    }

    public void registrarFalha(String ip, String email) {
        if (falhas.size() > LIMPAR_ACIMA_DE) {
            falhas.values().removeIf(this::expirou);
        }
        incrementar(chave(ip, email));
        incrementar(ip);
    }

    public void limpar(String ip, String email) {
        falhas.remove(chave(ip, email));
    }

    /** Barra o POST /login antes de checar a senha quando o par ou o IP esta bloqueado. */
    OncePerRequestFilter filtro() {
        return new OncePerRequestFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return !POST_LOGIN.matches(request);
            }

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                if (bloqueado(request.getRemoteAddr(), request.getParameter("username"))) {
                    REDIRECT.sendRedirect(request, response, "/login?erro=muitas-tentativas");
                    return;
                }
                chain.doFilter(request, response);
            }
        };
    }

    private static String chave(String ip, String email) {
        return ip + "|" + (email == null ? "" : email.trim().toLowerCase(Locale.ROOT));
    }

    private boolean expirou(Falhas f) {
        return !relogio.instant().isBefore(f.inicio().plus(JANELA));
    }

    private int total(String chave) {
        Falhas f = falhas.get(chave);
        return f == null || expirou(f) ? 0 : f.total();
    }

    private void incrementar(String chave) {
        falhas.compute(chave, (k, f) -> f == null || expirou(f)
                ? new Falhas(relogio.instant(), 1)
                : new Falhas(f.inicio(), f.total() + 1));
    }
}
