package com.curso.gestaoinvestimentos.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LiquidGlassMarkupTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> PAGES = List.of("index.html", "login.html", "dashboard.html");

    private String template(String name) throws IOException {
        return Files.readString(RESOURCES.resolve("templates").resolve(name));
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
            assertTrue(html.contains("localStorage.getItem('rendo-theme')"), page);
            assertTrue(html.indexOf("localStorage.getItem('rendo-theme')") < html.indexOf("@{/css/tokens.css}"), page);
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
        assertTrue(login.contains("class=\"form-container sign-in\""));
        assertTrue(dashboard.contains("id=\"sidebar\""));
        assertTrue(dashboard.contains("id=\"view-visao-geral\""));
        assertTrue(dashboard.contains("id=\"modalContent\""));
        assertTrue(dashboard.contains("data-view=\"visao-geral\""));
    }
}
