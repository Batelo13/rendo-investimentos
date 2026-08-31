package com.curso.gestaoinvestimentos.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardAcoesMarkupTest {

    private static final Path DASHBOARD_JS =
            Path.of("src/main/resources/static/js/dashboard.js");

    @Test
    void mantemFlexDoTickerDentroDeUmaCelulaDeTabela() throws IOException {
        String script = Files.readString(DASHBOARD_JS);

        assertTrue(script.contains(
                "<td><div class=\"acao-ticker-col\">${acaoLogoHTML(a)}${esc(a.ticker)}</div></td>"));
        assertFalse(script.contains("<td class=\"acao-ticker-col\">"));
    }
}
