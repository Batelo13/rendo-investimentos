package com.curso.gestaoinvestimentos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Modulo 11 (secao 15, achados do OWASP ZAP): cabecalhos de seguranca e
 * atributos do cookie de sessao. Servidor real (porta aleatoria), porque o
 * cookie de sessao e emitido pelo container, nao pelo MockMvc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityHeadersIntegrationTest {

    @LocalServerPort
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<Void> get(String caminho) throws Exception {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build();
        return http.send(requisicao, HttpResponse.BodyHandlers.discarding());
    }

    @Test
    @DisplayName("Scenario: Pagina publica com cabecalhos de seguranca")
    void paginaPublicaTemCabecalhosDeSeguranca() throws Exception {
        HttpResponse<Void> resposta = get("/");

        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(resposta.headers().firstValue("Referrer-Policy")).hasValue("strict-origin-when-cross-origin");
        assertThat(resposta.headers().firstValue("Cross-Origin-Resource-Policy")).hasValue("same-origin");
        assertThat(resposta.headers().firstValue("Permissions-Policy")).hasValue("camera=(), microphone=(), geolocation=()");
        assertThat(resposta.headers().firstValue("Cross-Origin-Opener-Policy")).hasValue("same-origin");
        assertThat(resposta.headers().firstValue("Content-Security-Policy")).hasValueSatisfying(csp -> assertThat(csp)
                .contains("default-src 'self'", "script-src 'self';", "form-action 'self'", "object-src 'none'")
                .doesNotContain("'unsafe-eval'")
                .doesNotContainPattern("script-src[^;]*'unsafe-inline'"));
    }

    @Test
    @DisplayName("Scenario: Console H2 de desenvolvimento fora da CSP restrita")
    void consoleH2NaoRecebeCspRestrita() throws Exception {
        assertThat(get("/h2-console/").headers().firstValue("Content-Security-Policy")).isEmpty();
    }

    @Test
    @DisplayName("Scenario: Assets da pagina inicial sao publicos")
    void videosDaPaginaInicialSaoPublicos() throws Exception {
        // Sem login: o video da jornada e conteudo publico como /images e /css
        // (arquivo ficticio em src/test/resources/static/videos).
        assertThat(get("/videos/teste-publico.mp4").statusCode()).isEqualTo(200);
        assertThat(get("/js/landing.js").statusCode()).isEqualTo(200);
        assertThat(get("/css/landing.css").statusCode()).isEqualTo(200);
        assertThat(get("/images/rendo-jornada-poster.webp").statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Scenario: Arquivo estatico publico inexistente")
    void estaticoInexistenteDevolve404() throws Exception {
        assertThat(get("/css/nao-existe.css").statusCode()).isEqualTo(404);
        assertThat(get("/images/nao-existe.png").statusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("Scenario: Rota protegida inexistente continua exigindo login")
    void rotaProtegidaInexistenteSemSessaoDevolve401() throws Exception {
        // Liberar /error nao pode revelar quais rotas protegidas existem.
        assertThat(get("/rota-que-nao-existe").statusCode()).isEqualTo(401);
    }

    @Test
    void apiSemSessaoTambemRecebeCabecalhos() throws Exception {
        HttpResponse<Void> resposta = get("/carteiras/me");

        assertThat(resposta.statusCode()).isEqualTo(401);
        assertThat(resposta.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");
        assertThat(resposta.headers().firstValue("Cross-Origin-Resource-Policy")).hasValue("same-origin");
    }

    @Test
    void cookieDeSessaoEhHttpOnlyESameSiteLax() throws Exception {
        // A tela de login grava o token CSRF na sessao, o que cria o cookie.
        HttpResponse<Void> resposta = get("/login");

        String cookie = resposta.headers().allValues("Set-Cookie").stream()
                .filter(c -> c.startsWith("JSESSIONID="))
                .findFirst().orElseThrow();
        assertThat(cookie).containsIgnoringCase("HttpOnly").containsIgnoringCase("SameSite=Lax");
    }
}
