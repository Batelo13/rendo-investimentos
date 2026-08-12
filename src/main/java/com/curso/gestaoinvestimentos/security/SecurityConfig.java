package com.curso.gestaoinvestimentos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF desativado por enquanto: ainda nao existem formularios Thymeleaf
                // para carregar o token automaticamente. Reativar na fase de frontend.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Landing page publica (secao 22 do escopo: paginas publicas) + os
                        // assets estaticos que ela carrega (CSS/imagens).
                        .requestMatchers(HttpMethod.GET, "/", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // Dashboard mostra saldo/posicoes do usuario -- nao pode ser publica
                        // como "/", senao um visitante sem sessao veria a casca da pagina.
                        .requestMatchers(HttpMethod.GET, "/dashboard").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/usuarios/*/bloquear", "/usuarios/*/desbloquear").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/carteiras/me", "/carteiras/me/operacoes", "/carteiras/me/saldo").authenticated()
                        .requestMatchers(HttpMethod.GET, "/carteiras/*", "/carteiras/*/operacoes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/carteiras/*/reconstruir").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/operacoes/*/cancelar").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Pagina de login propria (templates/login.html) no lugar da pagina
                // padrao gerada pelo Spring Security. URL de processamento do POST
                // continua sendo /login (mesmo comportamento de antes -- os testes que
                // ja faziam post("/login") nao mudam).
                .formLogin(form -> form.loginPage("/login").permitAll().defaultSuccessUrl("/dashboard", true))
                .logout(Customizer.withDefaults())
                // Ainda somos uma API testada via curl/JSON pra maior parte das rotas:
                // preferimos 401 a um redirect HTML para /login em recurso protegido.
                // Excecao: paginas server-side (ex.: /dashboard) sao acessadas por um
                // navegador de verdade -- ali um 401 em branco e uma tela quebrada, o
                // usuario precisa ser mandado pro formulario de login.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(paginaOuApiEntryPoint()))
                // H2 console roda dentro de um <frame>; sem isso o navegador bloqueia.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    // Rotas de pagina (PaginaController) sem sessao viram redirect pro /login;
    // qualquer outra rota (a API REST) continua retornando 401 puro.
    private AuthenticationEntryPoint paginaOuApiEntryPoint() {
        RequestMatcher paginas = PathPatternRequestMatcher.pathPattern("/dashboard");
        LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> porRota = new LinkedHashMap<>();
        porRota.put(paginas, new LoginUrlAuthenticationEntryPoint("/login"));

        DelegatingAuthenticationEntryPoint entryPoint = new DelegatingAuthenticationEntryPoint(porRota);
        entryPoint.setDefaultEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        return entryPoint;
    }
}
