# Alinhamento da tabela de ações Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restaurar o alinhamento da linha do catálogo mantendo o layout flexível de logotipo e ticker dentro de uma célula de tabela normal.

**Architecture:** `renderAcoes()` continuará gerando a linha, mas o `<td>` do ticker voltará a usar seu comportamento nativo. Um `<div class="acao-ticker-col">` interno concentrará exclusivamente o alinhamento flexível do logotipo e do texto.

**Tech Stack:** Java 17, Spring Boot 4.1, JavaScript, CSS, JUnit 5 e Maven.

---

### Task 1: Criar a regressão estrutural

**Files:**
- Create: `src/test/java/com/curso/gestaoinvestimentos/ui/DashboardAcoesMarkupTest.java`
- Test: `src/test/java/com/curso/gestaoinvestimentos/ui/DashboardAcoesMarkupTest.java`

- [ ] **Step 1: Escrever o teste que exige o wrapper interno**

```java
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
```

- [ ] **Step 2: Executar o teste e confirmar a falha esperada**

Run: `mvn -Dtest=DashboardAcoesMarkupTest test`

Expected: `FAIL`, porque `dashboard.js` ainda aplica `acao-ticker-col` diretamente ao `<td>`.

### Task 2: Restaurar a semântica da célula

**Files:**
- Modify: `src/main/resources/static/js/dashboard.js:443-452`
- Modify: `src/main/resources/static/css/dashboard.css:217`
- Test: `src/test/java/com/curso/gestaoinvestimentos/ui/DashboardAcoesMarkupTest.java`

- [ ] **Step 1: Mover o flex para um wrapper interno**

Substituir a célula do ticker em `renderAcoes()` por:

```javascript
<td><div class="acao-ticker-col">${acaoLogoHTML(a)}${esc(a.ticker)}</div></td>
```

- [ ] **Step 2: Documentar no CSS que a classe pertence ao wrapper**

Manter a regra existente e acrescentar o comentário:

```css
/* Wrapper interno: o <td> deve conservar o comportamento de table-cell. */
.acao-ticker-col { display: flex; align-items: center; gap: 8px; }
```

- [ ] **Step 3: Executar o teste específico**

Run: `mvn -Dtest=DashboardAcoesMarkupTest test`

Expected: `BUILD SUCCESS`, com 1 teste e 0 falhas.

- [ ] **Step 4: Executar toda a suíte**

Run: `mvn test`

Expected: `BUILD SUCCESS`, sem falhas nem erros.

- [ ] **Step 5: Verificar a página reconstruída**

Run: `docker compose up --build -d`

Abrir `http://localhost:8080`, acessar **Ações** e confirmar que ticker, empresa, mercado, cotação e botões compartilham a mesma linha e que os divisores permanecem contínuos.

- [ ] **Step 6: Versionar a correção**

```powershell
git add -- src/test/java/com/curso/gestaoinvestimentos/ui/DashboardAcoesMarkupTest.java src/main/resources/static/js/dashboard.js src/main/resources/static/css/dashboard.css
git commit -m "fix: alinha linha do catálogo de ações"
```
