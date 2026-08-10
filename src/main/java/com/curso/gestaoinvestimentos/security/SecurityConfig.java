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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

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
                        .requestMatchers(HttpMethod.GET, "/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/usuarios/*/bloquear", "/usuarios/*/desbloquear").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/carteiras/me", "/carteiras/me/operacoes", "/carteiras/me/saldo").authenticated()
                        .requestMatchers(HttpMethod.GET, "/carteiras/*", "/carteiras/*/operacoes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/carteiras/*/reconstruir").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/operacoes/*/cancelar").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults())
                .logout(Customizer.withDefaults())
                // Ainda somos uma API testada via curl/JSON: sem pagina de login propria,
                // preferimos 401 a um redirect HTML para /login em recurso protegido.
                // GET /login continua acessivel normalmente para quem quiser ver a tela padrao.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                // H2 console roda dentro de um <frame>; sem isso o navegador bloqueia.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
