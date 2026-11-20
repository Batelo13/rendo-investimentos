package com.curso.gestaoinvestimentos.security;

import com.curso.gestaoinvestimentos.security.oauth2.LoginSocialFailureHandler;
import com.curso.gestaoinvestimentos.security.oauth2.RendoOidcUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.CrossOriginOpenerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.LinkedHashMap;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Google Fonts (CSS + arquivos de fonte) e logos de acoes externos (https:)
    // sao as unicas origens de fora; 'unsafe-inline' so em estilo (atributos
    // style= gerados pelo JS e pelo SweetAlert2), nunca em script.
    static final String CSP = "default-src 'self'; script-src 'self'; "
            + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
            + "font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https:; "
            + "object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                     Optional<ClientRegistrationRepository> clientRegistrationRepository,
                                                     RendoOidcUserService rendoOidcUserService,
                                                     LoginSocialFailureHandler loginSocialFailureHandler,
                                                     LoginFormFailureHandler loginFormFailureHandler) throws Exception {
        http
                // CSRF reativado: login/logout sao forms Thymeleaf nativos (hidden
                // input com o token) e o dashboard/cadastro mandam o token via header
                // (meta tag lida pelo JS). H2 console tem forms proprios sem token,
                // por isso fica de fora da protecao.
                .csrf(csrf -> csrf.ignoringRequestMatchers(PathPatternRequestMatcher.pathPattern("/h2-console/**")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/usuarios").permitAll()
                        // Fluxo de verificacao de email: o usuario ainda nao tem sessao
                        // nesse ponto (a conta acabou de ser criada como nao verificada).
                        .requestMatchers(HttpMethod.POST, "/usuarios/verificar-email", "/usuarios/reenviar-codigo").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        // Fluxo de login social (so existe de fato quando ha provedor
                        // configurado -- ver OAuth2ClientRegistrations): precisa ser publico
                        // porque e o proprio caminho que leva o usuario a se autenticar.
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        // Landing page publica (secao 22 do escopo: paginas publicas) + os
                        // assets estaticos que ela carrega (CSS/imagens/video da jornada).
                        .requestMatchers(HttpMethod.GET, "/", "/css/**", "/js/**", "/images/**", "/videos/**", "/webjars/**").permitAll()
                        // formLogin().permitAll() so libera "/login" com query string EXATA
                        // (o "?error"/"?logout" que ele mesmo gera) -- qualquer outra query
                        // string (ex.: "?criarConta=1" do fluxo de login social) cai no
                        // anyRequest().authenticated() e devolve 401 sem essa regra explicita.
                        .requestMatchers(HttpMethod.GET, "/login").permitAll()
                        // Pagina de erro do Spring Boot: sem isso o dispatch interno para
                        // /error de um estatico publico inexistente caia no
                        // anyRequest().authenticated() e virava 401 em vez de 404. Rotas
                        // protegidas sem sessao continuam 401 antes de chegar aqui.
                        .requestMatchers("/error").permitAll()
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
                .formLogin(form -> form.loginPage("/login").permitAll().defaultSuccessUrl("/dashboard", true)
                        .failureHandler(loginFormFailureHandler))
                // permitAll(): sem isso, o redirect padrao de logout ("/login?logout")
                // nao fica na lista de URLs liberadas (essa lista compara a URL inteira,
                // incluindo query string, contra "/login" -- "/login?logout" nao bate) e
                // cai no anyRequest().authenticated(), devolvendo 401 em vez da tela de login.
                .logout(logout -> logout.permitAll())
                // Ainda somos uma API testada via curl/JSON pra maior parte das rotas:
                // preferimos 401 a um redirect HTML para /login em recurso protegido.
                // Excecao: paginas server-side (ex.: /dashboard) sao acessadas por um
                // navegador de verdade -- ali um 401 em branco e uma tela quebrada, o
                // usuario precisa ser mandado pro formulario de login.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(paginaOuApiEntryPoint()))
                // H2 console roda dentro de um <frame>; sem isso o navegador bloqueia.
                // Demais cabecalhos: achados do OWASP ZAP no Modulo 11 (secao 15).
                // CSP restrita (achado ZAP 10055, change dast-zap-baseline): nenhum
                // script inline nas paginas. Fora do /h2-console, que so existe no
                // profile dev e depende de script inline proprio.
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions.policy(
                                "camera=(), microphone=(), geolocation=()"))
                        .crossOriginResourcePolicy(corp -> corp.policy(
                                CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN))
                        .crossOriginOpenerPolicy(coop -> coop.policy(
                                CrossOriginOpenerPolicyHeaderWriter.CrossOriginOpenerPolicy.SAME_ORIGIN))
                        .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                new NegatedRequestMatcher(PathPatternRequestMatcher.pathPattern("/h2-console/**")),
                                new StaticHeadersWriter("Content-Security-Policy", CSP))));

        // So ativa oauth2Login() quando ha pelo menos um provedor configurado
        // (ver OAuth2ClientRegistrations) -- sem isso, chamar .oauth2Login() sem
        // nenhum ClientRegistrationRepository derrubaria o boot da aplicacao.
        clientRegistrationRepository.ifPresent(repository -> {
            try {
                http.oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureHandler(loginSocialFailureHandler)
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(rendoOidcUserService)));
            } catch (Exception e) {
                throw new IllegalStateException("Falha ao configurar oauth2Login", e);
            }
        });

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
