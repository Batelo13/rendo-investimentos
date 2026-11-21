package com.curso.gestaoinvestimentos.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidGlassMarkupTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> PAGES = List.of("index.html", "login.html", "dashboard.html");

    private String template(String name) throws IOException {
        return Files.readString(RESOURCES.resolve("templates").resolve(name));
    }

    private String dashboardScript() throws IOException {
        return Files.readString(RESOURCES.resolve("static/js/dashboard.js"));
    }

    private String loginScript() throws IOException {
        return Files.readString(RESOURCES.resolve("static/js/login.js"));
    }

    private String dashboardCss() throws IOException {
        return Files.readString(RESOURCES.resolve("static/css/dashboard.css"));
    }

    private String tokensCss() throws IOException {
        return Files.readString(RESOURCES.resolve("static/css/tokens.css"));
    }

    @Test
    void todasAsPaginasCarregamGlassAntesDoCssEspecificoEMotionAntesDoScriptDaTela() throws IOException {
        for (String page : PAGES) {
            String html = template(page);
            assertTrue(html.contains("@{/css/glass.css}"), page);
            assertTrue(html.contains("@{/js/motion.js}"), page);
            assertTrue(html.indexOf("@{/css/tokens.css}") < html.indexOf("@{/css/glass.css}"), page);
            assertTrue(html.indexOf("@{/js/loading.js}") < html.indexOf("@{/js/motion.js}"), page);
        }
    }

    @Test
    void paginasAplicamTemaAntesDosStylesheets() throws IOException {
        for (String page : PAGES) {
            String html = template(page);
            // Script externo e sincrono (CSP script-src 'self'): nenhum <script> inline.
            assertTrue(html.contains("@{/js/tema-inicial.js}"), page);
            assertTrue(html.indexOf("@{/js/tema-inicial.js}") < html.indexOf("@{/css/tokens.css}"), page);
            assertFalse(html.contains("<script>"), page);
            assertFalse(html.matches("(?s).*\\son[a-z]+=\".*"), page);
        }
    }

    @Test
    void paginasPossuemRaizVisualEAlvosDeMovimentoProgressivo() throws IOException {
        assertTrue(template("index.html").contains("class=\"rendo-page landing-page\""));
        assertTrue(template("login.html").contains("class=\"rendo-page auth-page\""));
        assertTrue(template("dashboard.html").contains("class=\"rendo-page dashboard-page\""));
        for (String page : PAGES) {
            assertTrue(template(page).contains("data-motion"), page);
        }
    }

    @Test
    void loadingInicialContinuaSomenteNoIndexEDashboard() throws IOException {
        assertTrue(template("index.html").contains("loading(startHidden=false)"));
        assertTrue(template("dashboard.html").contains("loading(startHidden=false)"));
        assertTrue(template("login.html").contains("loading(startHidden=true)"));
        String loadingJs = Files.readString(RESOURCES.resolve("static/js/loading.js"));
        assertTrue(loadingJs.contains("addEventListener('load'"));
        assertTrue(loadingJs.contains("classList.add('hidden')"));
        assertTrue(loadingJs.contains("dispatchEvent(new CustomEvent('rendo:page-ready'))"));
        assertTrue(loadingJs.contains("{ once: true }"));
        String loadingCss = Files.readString(RESOURCES.resolve("static/css/loading.css"));
        assertTrue(loadingCss.contains("display: flex !important;"));
    }

    @Test
    void contratosFuncionaisContinuamPresentes() throws IOException {
        String login = template("login.html");
        String dashboard = template("dashboard.html");
        assertTrue(login.contains("id=\"container\""));
        assertTrue(login.contains("id=\"cadastro-form\""));
        assertTrue(login.contains("id=\"verificacao-form\""));
        assertTrue(login.contains("id=\"btn-cadastro\""));
        assertTrue(login.contains("id=\"btn-login\""));
        assertTrue(login.contains("name=\"username\""));
        assertTrue(login.contains("name=\"password\""));
        assertTrue(login.contains("class=\"form-container sign-in\""));
        assertTrue(dashboard.contains("id=\"sidebar\""));
        assertTrue(dashboard.contains("id=\"view-visao-geral\""));
        assertTrue(dashboard.contains("id=\"modalContent\""));
        assertTrue(dashboard.contains("data-view=\"visao-geral\""));
        assertTrue(dashboard.contains("id=\"refreshAll\""));
        assertTrue(dashboard.contains("id=\"rendimentoChart\""));
        assertTrue(dashboard.contains("id=\"periodoPills\""));
        assertTrue(dashboard.contains("id=\"menuToggle\""));
        assertTrue(dashboard.contains("id=\"modal\""));
        assertTrue(dashboard.contains("id=\"toasts\""));
    }

    @Test
    void modalTemEntradaESaidaAnimadasSemAlterarSeuId() throws IOException {
        String script = dashboardScript();
        assertTrue(script.contains("classList.add(\"open\")"));
        assertTrue(script.contains("transitionend"));
        assertTrue(script.contains("classList.add(\"hidden\")"));
    }

    @Test
    void dashboardExpoeModalESidebarMoveisAcessiveis() throws IOException {
        String dashboard = template("dashboard.html");
        assertTrue(dashboard.contains("role=\"dialog\""));
        assertTrue(dashboard.contains("aria-modal=\"true\""));
        assertTrue(dashboard.contains("aria-label=\"Detalhes do investimento\""));
        assertTrue(dashboard.contains("tabindex=\"-1\""));
        assertTrue(dashboard.contains("id=\"modalClose\""));
        assertTrue(dashboard.contains("aria-label=\"Fechar janela\""));
        assertTrue(dashboard.contains("aria-controls=\"sidebar\""));
        assertTrue(dashboard.contains("aria-expanded=\"false\""));
        assertTrue(dashboard.contains("id=\"sidebarClose\""));
        assertTrue(dashboard.contains("aria-label=\"Fechar menu\""));
        assertTrue(dashboard.contains("id=\"sidebarBackdrop\""));
        assertTrue(dashboard.contains("aria-label=\"Fechar menu lateral\""));
    }

    @Test
    void dashboardProtegeLifecycleFocoETecladoDeModalESidebar() throws IOException {
        String script = dashboardScript();
        assertTrue(script.contains("event.target !== modal"));
        assertTrue(script.contains("event.propertyName !== \"opacity\""));
        assertTrue(script.contains("removeEventListener(\"transitionend\""));
        assertTrue(script.contains("cancelAnimationFrame"));
        assertTrue(script.contains("modalGeneration"));
        assertTrue(script.contains("modalClosing"));
        assertTrue(script.contains("modalPreviouslyFocused"));
        assertTrue(script.contains("event.key === \"Tab\""));
        assertTrue(script.contains("event.key === \"Escape\""));
        assertTrue(script.contains("aria-expanded"));
        assertTrue(script.contains("toggleAttribute(\"inert\""));
        assertTrue(script.contains("aria-hidden"));
        assertTrue(script.contains("matchMedia(\"(max-width: 860px)\")"));
        assertTrue(script.contains("function syncSidebarForMediaChange"));
        assertTrue(script.contains("focusStableDashboardTarget(focusTarget)"));
        assertFalse(script.contains("addEventListener(\"change\", () => setSidebarOpen(false, { focus: false, restoreFocus: true }))"));
        assertTrue(script.contains("getComputedStyle(document.documentElement)"));
        assertTrue(script.contains("\"--success-text\""));
        assertTrue(script.contains("\"--danger-text\""));
        assertFalse(script.contains("renderGraficoRendimento();"));
    }

    @Test
    void cardsDeCorretoraSaoAcionaveisPorCliqueETecladoComUmaSoAcao() throws IOException {
        String script = dashboardScript();
        assertTrue(script.contains("role=\"button\" tabindex=\"0\" data-detail-corretora"));
        assertTrue(script.contains("function abrirDetalheCorretora(card)"));
        assertTrue(script.contains("event.target.closest(\"[data-detail-corretora]\")"));
        assertTrue(script.contains("event.key === \"Enter\""));
        assertTrue(script.contains("event.key === \" \""));
        assertTrue(script.contains("event.preventDefault()"));
        assertTrue(script.contains("abrirDetalheCorretora(card)"));
        assertTrue(script.contains("abrirDetalheCorretora(dc)"));
    }

    @Test
    void filtroDeCorretorasSoComparaCnpjQuandoOTermoPossuiDigitos() throws IOException {
        String script = dashboardScript();

        assertTrue(script.contains("const filtroCnpj = onlyDigits(filtro);"));
        assertTrue(script.contains("(filtroCnpj && onlyDigits(c.cnpj).includes(filtroCnpj))"));
        assertFalse(script.contains("onlyDigits(c.cnpj).includes(onlyDigits(filtro))"));
    }

    @Test
    void overviewFinanceiroEmpilhaAntesDeCortarMoedaEmViewportEstreito() throws IOException {
        String css = dashboardCss();
        assertTrue(css.matches("(?s).*@media \\(max-width: 420px\\) \\{.*"
                + "\\.stats-grid \\{[^}]*grid-template-columns: minmax\\(0, 1fr\\);[^}]*}.*"));
        assertTrue(css.matches("(?s).*@media \\(max-width: 420px\\) \\{.*"
                + "\\.stats-grid > \\* \\{[^}]*min-width: 0;[^}]*}.*"));
        assertTrue(css.matches("(?s).*@media \\(max-width: 420px\\) \\{.*"
                + "\\.stat-value \\{[^}]*font-size: clamp\\(16px, 5vw, 20px\\);[^}]*white-space: nowrap;[^}]*}.*"));
    }

    @Test
    void landingUsaContadorNosFatos() throws IOException {
        assertTrue(template("index.html").contains("data-contador=\"100000\" data-formato=\"brl0\""));
        String motion = Files.readString(RESOURCES.resolve("static/js/motion.js"));
        for (String contrato : List.of("RendoMotion", "data-contador", "data-luz", "data-inclina")) {
            assertTrue(motion.contains(contrato), contrato);
        }
    }

    @Test
    void aberturaTemCartaoDemonstrativo() throws IOException {
        String index = template("index.html");
        for (String contrato : List.of("id=\"cenaCarteira\"", "Dados demonstrativos",
                "data-inclina=\"6\"", "data-inclina-area=\"#jornadaPalco\"", "id=\"jornadaPalco\"")) {
            assertTrue(index.contains(contrato), contrato);
        }
        assertFalse(index.contains("id=\"cenaPessoa\""));
        assertFalse(index.contains("id=\"cenaCidade\""));
        assertEquals(1, index.split("<h1", -1).length - 1, "um unico h1");
    }

    @Test
    void landingTemSimuladorDemonstrativo() throws IOException {
        String index = template("index.html");
        for (String contrato : List.of("id=\"experimente\"", "href=\"#experimente\"", "Dados demonstrativos",
                "Série ilustrativa: não é histórico real nem previsão.", "US$ 1 = R$ 5,40 (cotação demonstrativa)",
                "aria-live=\"polite\"", "<noscript>")) {
            assertTrue(index.contains(contrato), contrato);
        }
        assertTrue(index.indexOf("@{/js/landing.js}") < index.indexOf("@{/js/landing-simulador.js}"));
        assertEquals(3, index.split("name=\"cenario\"", -1).length - 1, "tres cenarios");
    }

    @Test
    void landingTemSecaoProblema() throws IOException {
        String index = template("index.html");
        assertTrue(index.contains("id=\"problema\""));
        assertEquals(3, index.split("data-resolve", -1).length - 1, "tres cards");
        int problema = index.indexOf("id=\"problema\"");
        assertTrue(index.indexOf("id=\"visao-geral\"") < problema && problema < index.indexOf("id=\"experimente\""));
        assertTrue(Files.readString(RESOURCES.resolve("static/js/motion.js")).contains("data-resolve"));
    }

    @Test
    void landingTemTourDoProduto() throws IOException {
        String index = template("index.html");
        assertEquals(4, index.split("class=\"tour-passo\"", -1).length - 1, "quatro passos");
        for (String contrato : List.of("id=\"tour-1\"", "id=\"tour-2\"", "id=\"tour-3\"", "id=\"tour-4\"",
                "Conferido na CVM", "Revise sua ordem")) {
            assertTrue(index.contains(contrato), contrato);
        }
        assertFalse(index.contains("lp-acompanhamento"));
        assertTrue(index.indexOf("@{/js/landing-simulador.js}") < index.indexOf("@{/js/landing-tour.js}"));
    }

    @Test
    void landingTemDiferenciaisDemonstrados() throws IOException {
        String index = template("index.html");
        assertEquals(3, index.split("class=\"dif-bloco", -1).length - 1, "tres blocos");
        for (String contrato : List.of("Cotação ilustrativa", "Valores de exemplo", "Também na Rendo:")) {
            assertTrue(index.contains(contrato), contrato);
        }
        String nav = index.substring(index.indexOf("<nav class=\"lp-nav\""), index.indexOf("</nav>", index.indexOf("<nav class=\"lp-nav\"")));
        assertFalse(nav.contains("href=\"#recursos\""), "cabecalho sem o link Recursos");
        assertTrue(index.indexOf("@{/js/landing-tour.js}") < index.indexOf("@{/js/landing-diferenciais.js}"));
    }

    @Test
    void landingTemSegurancaECtaFinal() throws IOException {
        String index = template("index.html");
        int inicioSeguranca = index.indexOf("id=\"seguranca\"");
        String seguranca = index.substring(inicioSeguranca, index.indexOf("</section>", inicioSeguranca));
        assertEquals(7, seguranca.split("<li", -1).length - 1, "sete protecoes");
        assertTrue(seguranca.contains("Nenhum dinheiro real circula na Rendo."));
        assertTrue(seguranca.contains("Limite de tentativas"));
        // A janela conta da primeira falha: o texto nao pode prometer 15 min de bloqueio.
        assertTrue(seguranca.contains("Após 5 senhas erradas em 15 minutos, o login fica bloqueado."));
        int inicioCta = index.indexOf("class=\"lp-cta\"");
        String cta = index.substring(inicioCta, index.indexOf("</section>", inicioCta));
        assertTrue(cta.contains("data-contador=\"100000\""));
        assertTrue(cta.contains("data-ima"));
        assertTrue(Files.readString(RESOURCES.resolve("static/js/motion.js")).contains("data-ima"));
    }

    @Test
    void landingTemOpenGraph() throws IOException {
        String index = template("index.html");
        assertTrue(index.contains("property=\"og:title\""));
        assertTrue(index.contains("property=\"og:description\""));
        assertTrue(index.contains("property=\"og:type\" content=\"website\""));
    }

    @Test
    void landingNaoTemVideo() throws IOException {
        String index = template("index.html");
        assertFalse(index.contains("<video"));
        assertFalse(index.contains("data-video"));
        assertFalse(index.contains("jornada-poster"));
        assertFalse(Files.readString(RESOURCES.resolve("static/js/landing.js")).contains("video"));
    }

    @Test
    void tituloDoGraficoTemOMesmoTamanhoDosOutrosPaineis() throws IOException {
        // Sem isso o h2 de .chart-head cai no h2 do Bootstrap (~32px).
        assertTrue(dashboardCss().matches("(?s).*\\.chart-head h2[^{]*\\{[^}]*font-size: 16px;.*"));
    }

    @Test
    void dashboardMantemContrasteSemanticoECabeEmViewportMovel() throws IOException {
        String css = dashboardCss();
        String tokens = tokensCss();
        assertTrue(tokens.contains("--brand-text: #5DE2BA;"));
        assertTrue(tokens.contains("--success-text: #5DE2BA;"));
        assertTrue(tokens.contains("--danger-text: #FF8F8F;"));
        assertTrue(tokens.contains("--brand-text: #147A61;"));
        assertTrue(tokens.contains("--success-text: #146C43;"));
        assertTrue(tokens.contains("--danger-text: #B42318;"));
        assertTrue(css.contains("color: var(--brand-text);"));
        assertTrue(css.contains("color: var(--success-text);"));
        assertTrue(css.contains("color: var(--danger-text);"));
        assertTrue(css.contains("z-index: 100;"));
        assertTrue(css.matches("(?s).*@media \\(max-width: 860px\\) \\{.*"
                + "\\.cards \\{[^}]*grid-template-columns: minmax\\(0, 1fr\\);[^}]*}.*"));
        assertTrue(css.matches("(?s).*@media \\(max-width: 860px\\) \\{.*"
                + "\\.form-inline \\{[^}]*flex-direction: column;[^}]*}.*"));
    }

    @Test
    void autenticacaoUsaShellGlassForte() throws IOException {
        assertTrue(template("login.html").contains("class=\"login-container glass-surface-strong\""));
    }

    @Test
    void autenticacaoUsaTokensGlassEMantemPainelTematico() throws IOException {
        String css = Files.readString(RESOURCES.resolve("static/css/login.css"));
        assertTrue(css.contains("border-color: var(--glass-border-highlight);"));
        assertTrue(css.contains("background: var(--glass-standard);"));
        assertTrue(css.contains(":root[data-theme=\"light\"] .login-page"));
        assertTrue(css.contains("--auth-panel: rgba(45, 45, 70, 0.9);"));
        assertTrue(css.contains("var(--motion-medium) var(--ease-emphasized)"));
        assertTrue(css.contains("outline: 2px solid var(--focus-ring);"));
    }

    @Test
    void autenticacaoMantemContrasteFocoEAlvosMoveisAcessiveis() throws IOException {
        String css = Files.readString(RESOURCES.resolve("static/css/login.css"));
        assertTrue(css.contains("--auth-danger-text:"));
        assertTrue(css.contains("--auth-success-text:"));
        assertTrue(css.contains(".toggle-head button:focus-visible"));
        assertTrue(css.contains("outline: 2px solid var(--brand);"));
        assertTrue(css.contains(".login-container .link-discreto:hover { color: var(--focus-ring); }"));
        assertTrue(css.contains(".input-group.valido input { border-color: var(--auth-success-text); }"));
        assertTrue(css.contains(".input-group.invalido input { border-color: var(--auth-danger-text); }"));
        assertFalse(css.contains("min-height: 560px;"));
        assertTrue(css.matches(
                "(?s).*@media \\(max-width: 700px\\) \\{.*button\\.link-discreto,\\s*"
                        + "a\\.link-discreto \\{[^}]*min-height: 44px;[^}]*\\}.*"));
    }

    @Test
    void autenticacaoContemOverflowDecorativoSomenteNoEixoHorizontal() throws IOException {
        String css = Files.readString(RESOURCES.resolve("static/css/login.css"));
        String loginPageRule = css.substring(css.indexOf(".login-page {"), css.indexOf("}", css.indexOf(".login-page {")) + 1);

        assertTrue(loginPageRule.contains("overflow-x: clip;"));
        assertFalse(loginPageRule.contains("overflow: clip;"));
        assertFalse(loginPageRule.contains("overflow-y: hidden;"));
        assertFalse(loginPageRule.contains("overflow-y: clip;"));
    }

    @Test
    void autenticacaoOcultaFormularioInativoDuranteTrocaDePainel() throws IOException {
        String css = Files.readString(RESOURCES.resolve("static/css/login.css"));

        assertTrue(css.matches("(?s).*\\.sign-in \\{[^}]*opacity: 1;[^}]*"
                + "visibility: visible;[^}]*pointer-events: auto;[^}]*}.*"));
        assertTrue(css.matches("(?s).*\\.sign-up \\{[^}]*opacity: 0;[^}]*"
                + "visibility: hidden;[^}]*pointer-events: none;[^}]*}.*"));
        assertTrue(css.matches("(?s).*\\.login-container\\.active \\.sign-in \\{[^}]*"
                + "opacity: 0;[^}]*visibility: hidden;[^}]*pointer-events: none;[^}]*}.*"));
        assertTrue(css.matches("(?s).*\\.login-container\\.active \\.sign-up \\{[^}]*"
                + "opacity: 1;[^}]*visibility: visible;[^}]*pointer-events: auto;[^}]*}.*"));
    }

    @Test
    void autenticacaoCoordenaFocoEArvoreAcessivelComAnimacaoDoPainel() throws IOException {
        String login = template("login.html");
        String script = loginScript();

        assertTrue(login.contains("id=\"container\" tabindex=\"-1\""));
        assertTrue(login.contains("class=\"form-container sign-up\" inert aria-hidden=\"true\""));
        assertTrue(login.contains("class=\"form-container sign-in\" aria-hidden=\"false\""));
        assertTrue(login.contains("class=\"toggle-panel toggle-left\" inert aria-hidden=\"true\""));
        assertTrue(login.contains("class=\"toggle-panel toggle-right\" aria-hidden=\"false\""));
        assertTrue(script.contains("const authParams = new URLSearchParams(window.location.search);"));
        assertEquals(1, script.split("new URLSearchParams", -1).length - 1);
        assertTrue(script.contains("function bloquearRegioesAuth()"));
        assertTrue(script.contains("[signIn, signUp, toggleLeft, toggleRight].forEach"));
        assertTrue(script.contains("container.focus({ preventScroll: true });"));
        assertTrue(script.contains("toggle.addEventListener('transitionend', authTransitionHandler);"));
        assertTrue(script.contains("toggle.removeEventListener('transitionend', authTransitionHandler);"));
        assertTrue(script.contains("const AUTH_TRANSITION_FALLBACK_MARGIN_MS = 250;"));
        assertTrue(script.contains("duracaoMovimentoAuth() + AUTH_TRANSITION_FALLBACK_MARGIN_MS"));
        assertTrue(script.contains("window.matchMedia('(prefers-reduced-motion: reduce)').matches"));
        assertTrue(script.contains("window.matchMedia('(max-width: 700px)').matches"));
        assertTrue(script.contains("if (versao !== authTransitionVersion) return;"));
        assertTrue(script.contains("agendarFocoLogicoAuth(mostrarCadastro)"));
        assertTrue(script.contains("sincronizarEstadoAuth(cadastroViaSocial, { animar: false });"));
        assertTrue(script.contains("sincronizarEstadoAuth(true, { moverFoco: true })"));
        assertTrue(script.contains("sincronizarEstadoAuth(false, { moverFoco: true })"));
        assertFalse(script.contains("container.classList.remove('active')"));
        assertFalse(script.contains("container.classList.add('active')"));
    }
}
