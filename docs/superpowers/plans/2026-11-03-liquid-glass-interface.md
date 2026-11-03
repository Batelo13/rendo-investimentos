# Liquid Glass Interface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aplicar uma linguagem liquid glass discreta, profissional, responsiva e acessível em landing, autenticação, loading e dashboard, preservando a identidade e todos os fluxos funcionais do Rendo.

**Architecture:** `tokens.css` define valores semânticos por tema; o novo `glass.css` fornece primitives visuais e fallbacks; o novo `motion.js` adiciona movimento progressivo com Web Animations e `IntersectionObserver`, sem esconder HTML por padrão. Templates recebem apenas classes/atributos de apresentação, enquanto CSS e JavaScript específicos preservam IDs, `data-*`, endpoints e listeners existentes.

**Tech Stack:** Java 17, Spring Boot, Thymeleaf, Bootstrap 5.3.3 CSS, JavaScript vanilla, CSS custom properties, Web Animations API, IntersectionObserver, JUnit 5, OpenSpec e Graphify.

---

## File structure

- Create `src/main/resources/static/css/glass.css`: primitives de atmosfera, superfícies, interações, fallback e redução de movimento.
- Create `src/main/resources/static/js/motion.js`: entrada progressiva não bloqueante e stagger limitado.
- Create `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`: contratos de integração dos recursos visuais e preservação dos seletores funcionais.
- Modify `src/main/resources/static/css/tokens.css`: tokens glass/motion para os dois temas.
- Modify `src/main/resources/static/css/loading.css`: overlay glass e saída coordenada.
- Modify `src/main/resources/static/css/login.css`: autenticação temática em glass, preservando o painel deslizante.
- Modify `src/main/resources/static/css/dashboard.css`: dashboard em camadas, responsividade e componentes dinâmicos.
- Modify `src/main/resources/static/js/loading.js`: evento único `rendo:page-ready`.
- Modify `src/main/resources/templates/index.html`: landing glass e carregamento dos recursos compartilhados.
- Modify `src/main/resources/templates/login.html`: tema antecipado, recursos e atributos de movimento.
- Modify `src/main/resources/templates/dashboard.html`: classes/atributos de apresentação sem mudar contratos do DOM.
- Modify `src/main/resources/templates/fragments/loading.html`: semântica do overlay.
- Modify `openspec/changes/liquid-glass-interface/tasks.md`: marcar tarefas somente após verificação correspondente.

### Task 1: Lock the visual integration contracts

**Files:**
- Create: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`
- Reference: `src/main/resources/templates/index.html`
- Reference: `src/main/resources/templates/login.html`
- Reference: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: Write the failing markup contract test**

Create the test with this complete content:

```java
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
```

- [ ] **Step 2: Run the test and confirm the expected failure**

Run:

```powershell
.\mvnw.cmd -Dtest=LiquidGlassMarkupTest test
```

Expected: FAIL because `glass.css`, `motion.js`, `rendo-page` and `data-motion` are not yet referenced.

- [ ] **Step 3: Commit the red test**

```powershell
git add src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java
git commit -m "test(ui): define liquid glass integration contracts"
```

### Task 2: Build shared glass tokens and primitives

**Files:**
- Modify: `src/main/resources/static/css/tokens.css:18`
- Modify: `src/main/resources/static/css/tokens.css:63`
- Create: `src/main/resources/static/css/glass.css`
- Create: `src/main/resources/static/js/motion.js`

- [ ] **Step 1: Add dark-theme semantic tokens**

Insert after the existing `--transition-fast` declaration in the root block:

```css
    --atmosphere-brand: rgba(93, 226, 186, 0.14);
    --atmosphere-slate: rgba(92, 90, 132, 0.18);
    --glass-subtle-solid: #141820;
    --glass-standard-solid: #171C25;
    --glass-strong-solid: #11151D;
    --glass-subtle: rgba(20, 24, 32, 0.58);
    --glass-standard: rgba(23, 28, 37, 0.72);
    --glass-strong: rgba(17, 21, 29, 0.86);
    --glass-border: rgba(255, 255, 255, 0.09);
    --glass-border-highlight: rgba(255, 255, 255, 0.16);
    --glass-inner-highlight: inset 0 1px 0 rgba(255, 255, 255, 0.07);
    --glass-shadow: 0 22px 60px -34px rgba(0, 0, 0, 0.72);
    --glass-shadow-raised: 0 28px 70px -34px rgba(0, 0, 0, 0.82);
    --glass-blur-subtle: 12px;
    --glass-blur: 18px;
    --glass-blur-strong: 24px;
    --motion-fast: 160ms;
    --motion-medium: 360ms;
    --motion-slow: 700ms;
    --ease-standard: cubic-bezier(0.2, 0.8, 0.2, 1);
    --ease-emphasized: cubic-bezier(0.16, 1, 0.3, 1);
```

- [ ] **Step 2: Add equivalent light-theme tokens**

Append inside `:root[data-theme="light"]`:

```css
    --atmosphere-brand: rgba(55, 190, 151, 0.18);
    --atmosphere-slate: rgba(96, 93, 145, 0.14);
    --glass-subtle-solid: #F7F9FA;
    --glass-standard-solid: #FFFFFF;
    --glass-strong-solid: #F8FAFB;
    --glass-subtle: rgba(255, 255, 255, 0.56);
    --glass-standard: rgba(255, 255, 255, 0.74);
    --glass-strong: rgba(255, 255, 255, 0.9);
    --glass-border: rgba(16, 19, 26, 0.09);
    --glass-border-highlight: rgba(255, 255, 255, 0.92);
    --glass-inner-highlight: inset 0 1px 0 rgba(255, 255, 255, 0.9);
    --glass-shadow: 0 22px 55px -34px rgba(45, 54, 72, 0.3);
    --glass-shadow-raised: 0 28px 65px -32px rgba(45, 54, 72, 0.38);
```

- [ ] **Step 3: Create the shared glass stylesheet**

Create `glass.css` with these concrete primitives:

```css
body.rendo-page {
    min-height: 100vh;
    position: relative;
    isolation: isolate;
    overflow-x: hidden;
    background:
        radial-gradient(circle at 12% 8%, var(--atmosphere-brand), transparent 32rem),
        radial-gradient(circle at 88% 16%, var(--atmosphere-slate), transparent 34rem),
        var(--background);
    color: var(--text-primary);
}

body.rendo-page::before {
    content: "";
    position: fixed;
    inset: -18%;
    z-index: -1;
    pointer-events: none;
    background:
        radial-gradient(circle at 30% 70%, var(--atmosphere-slate), transparent 28rem),
        radial-gradient(circle at 74% 78%, var(--atmosphere-brand), transparent 26rem);
    opacity: 0.55;
    animation: rendo-atmosphere 24s ease-in-out infinite alternate;
}

.glass-surface,
.glass-surface-subtle,
.glass-surface-strong {
    border: 1px solid var(--glass-border);
    box-shadow: var(--glass-inner-highlight), var(--glass-shadow);
}

.glass-surface { background: var(--glass-standard-solid); }
.glass-surface-subtle { background: var(--glass-subtle-solid); }
.glass-surface-strong { background: var(--glass-strong-solid); }

@supports ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px))) {
    .glass-surface {
        background: var(--glass-standard);
        -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(125%);
        backdrop-filter: blur(var(--glass-blur)) saturate(125%);
    }
    .glass-surface-subtle {
        background: var(--glass-subtle);
        -webkit-backdrop-filter: blur(var(--glass-blur-subtle)) saturate(118%);
        backdrop-filter: blur(var(--glass-blur-subtle)) saturate(118%);
    }
    .glass-surface-strong {
        background: var(--glass-strong);
        -webkit-backdrop-filter: blur(var(--glass-blur-strong)) saturate(132%);
        backdrop-filter: blur(var(--glass-blur-strong)) saturate(132%);
    }
}

.glass-interactive {
    transition:
        transform var(--motion-fast) var(--ease-standard),
        border-color var(--motion-fast) ease,
        box-shadow var(--motion-fast) ease;
}

.glass-focus:focus-visible,
.glass-interactive:focus-visible {
    outline: 2px solid var(--brand);
    outline-offset: 3px;
}

@media (hover: hover) and (pointer: fine) {
    .glass-interactive:hover {
        transform: translateY(-4px);
        border-color: var(--glass-border-highlight);
        box-shadow: var(--glass-inner-highlight), var(--glass-shadow-raised);
    }
}

@keyframes rendo-atmosphere {
    from { transform: translate3d(-2%, -1%, 0) scale(1); }
    to { transform: translate3d(3%, 2%, 0) scale(1.04); }
}

@media (max-width: 700px) {
    body.rendo-page::before { opacity: 0.38; }
    .glass-surface,
    .glass-surface-subtle,
    .glass-surface-strong {
        --glass-blur-subtle: 8px;
        --glass-blur: 12px;
        --glass-blur-strong: 16px;
    }
}

@media (prefers-reduced-motion: reduce) {
    *, *::before, *::after {
        scroll-behavior: auto !important;
        animation-duration: 0.01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: 0.01ms !important;
    }
    body.rendo-page::before { animation: none; }
    .glass-interactive:hover { transform: none; }
}
```

- [ ] **Step 4: Create non-blocking progressive motion**

Create `motion.js`:

```javascript
(() => {
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const animated = new WeakSet();

    function reveal(element, index) {
        if (animated.has(element)) return;
        animated.add(element);
        if (reducedMotion.matches || typeof element.animate !== 'function') return;

        element.animate(
            [
                { opacity: 0.35, transform: 'translateY(12px)', filter: 'blur(6px)' },
                { opacity: 1, transform: 'translateY(0)', filter: 'blur(0)' }
            ],
            {
                duration: 520,
                delay: Math.min(index, 6) * 45,
                easing: 'cubic-bezier(0.16, 1, 0.3, 1)',
                fill: 'both'
            }
        );
    }

    function startMotion() {
        const targets = [...document.querySelectorAll('[data-motion]')];
        if (reducedMotion.matches || !('IntersectionObserver' in window)) {
            targets.forEach((element, index) => reveal(element, index));
            return;
        }

        const observer = new IntersectionObserver((entries) => {
            entries.forEach((entry) => {
                if (!entry.isIntersecting) return;
                const index = targets.indexOf(entry.target);
                reveal(entry.target, index);
                observer.unobserve(entry.target);
            });
        }, { threshold: 0.08, rootMargin: '0px 0px -4% 0px' });

        targets.forEach((element) => observer.observe(element));
    }

    window.addEventListener('rendo:page-ready', startMotion, { once: true });
    if (document.readyState === 'complete') startMotion();
})();
```

- [ ] **Step 5: Verify the static resources compile with the application**

Run:

```powershell
.\mvnw.cmd -DskipTests compile
```

Expected: Maven reports `BUILD SUCCESS`.

- [ ] **Step 6: Commit the shared foundation**

```powershell
git add src/main/resources/static/css/tokens.css src/main/resources/static/css/glass.css src/main/resources/static/js/motion.js
git commit -m "feat(ui): add shared liquid glass foundation"
```

### Task 3: Wire shared resources and make the contract test green

**Files:**
- Modify: `src/main/resources/templates/index.html:16-25`
- Modify: `src/main/resources/templates/login.html:19-22,24,26,300-301`
- Modify: `src/main/resources/templates/dashboard.html:19-22,24,26-27,85-100,364-367`
- Test: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`

- [ ] **Step 1: Load styles in the same order on every page**

Use this order in all three `<head>` elements:

```html
<link rel="stylesheet" th:href="@{/css/tokens.css}">
<link rel="stylesheet" th:href="@{/css/base.css}">
<link rel="stylesheet" th:href="@{/css/glass.css}">
<!-- page-specific stylesheet: login.css or dashboard.css when present -->
<link rel="stylesheet" th:href="@{/css/loading.css}">
```

Keep the existing pre-paint `rendo-theme` script before these links.

- [ ] **Step 2: Add page roots and progressive-motion targets**

Apply these exact root classes and attributes:

```html
<!-- index.html -->
<body class="rendo-page landing-page">
<main class="landing-shell glass-surface glass-surface-strong" data-motion>

<!-- login.html -->
<body class="rendo-page auth-page">
<main class="login-page" data-motion>

<!-- dashboard.html -->
<body class="rendo-page dashboard-page">
<div class="app glass-stage">
<aside class="sidebar glass-surface glass-surface-strong" id="sidebar" data-motion>
<header class="topbar glass-surface glass-surface-subtle" data-motion>
<section class="view" id="view-visao-geral" data-motion>
```

Add `data-motion` to each remaining top-level `.view`, not to table rows.

- [ ] **Step 3: Load scripts in a deterministic order**

All three pages load shared scripts in this order, followed by page-specific scripts:

```html
<script th:src="@{/js/loading.js}"></script>
<script th:src="@{/js/motion.js}"></script>
```

On dashboard keep `theme.js`, SweetAlert2 and `dashboard.js` after `motion.js`. On login keep `login.js` after `motion.js`.

- [ ] **Step 4: Run the contract tests**

```powershell
.\mvnw.cmd -Dtest=LiquidGlassMarkupTest,DashboardAcoesMarkupTest test
```

Expected: both test classes PASS.

- [ ] **Step 5: Commit resource integration**

```powershell
git add src/main/resources/templates/index.html src/main/resources/templates/login.html src/main/resources/templates/dashboard.html src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java
git commit -m "feat(ui): wire glass resources into every screen"
```

### Task 4: Coordinate landing and loading reveal

**Files:**
- Modify: `src/main/resources/templates/index.html:20-24`
- Modify: `src/main/resources/templates/fragments/loading.html:11-18`
- Modify: `src/main/resources/static/css/loading.css:7-38`
- Modify: `src/main/resources/static/js/loading.js:1-8`
- Test: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`

- [ ] **Step 1: Extend the test with loading invariants**

Add this method:

```java
    @Test
    void loadingInicialContinuaSomenteNoIndexEDashboard() throws IOException {
        assertTrue(template("index.html").contains("loading(startHidden=false)"));
        assertTrue(template("dashboard.html").contains("loading(startHidden=false)"));
        assertTrue(template("login.html").contains("loading(startHidden=true)"));
        String loading = Files.readString(RESOURCES.resolve("static/js/loading.js"));
        assertTrue(loading.contains("rendo:page-ready"));
    }
```

- [ ] **Step 2: Run the test and confirm it fails on the missing event**

```powershell
.\mvnw.cmd -Dtest=LiquidGlassMarkupTest#loadingInicialContinuaSomenteNoIndexEDashboard test
```

Expected: FAIL because `loading.js` does not dispatch `rendo:page-ready`.

- [ ] **Step 3: Replace the minimal index body with a finished landing composition**

Keep the loading fragment, then use:

```html
<main class="landing-shell glass-surface glass-surface-strong" data-motion>
    <div th:replace="~{fragments/logo :: logo}"></div>
    <span class="landing-eyebrow">Sua carteira, com clareza</span>
    <h1>Decisões melhores começam com uma visão completa.</h1>
    <p>Organize ativos, corretoras e operações em uma experiência simples, segura e feita para acompanhar sua evolução.</p>
    <a class="landing-cta glass-focus" th:href="@{/login}">Acessar minha carteira</a>
</main>
```

Add page-specific landing rules at the end of `base.css` for centered layout, `max-width: 760px`, `border-radius: 28px`, responsive padding, eyebrow and CTA using `--brand`/`--on-brand`.

- [ ] **Step 4: Make the loading overlay glass-aware and semantic**

Add `role="status" aria-live="polite" aria-label="Carregando"` to `#loadingOverlay`, mark the SVG `aria-hidden="true"`, and change the overlay CSS to:

```css
.loading-overlay {
    position: fixed;
    inset: 0;
    z-index: 9999;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--glass-strong-solid);
    transition: opacity var(--motion-medium) var(--ease-standard), visibility var(--motion-medium) linear;
}

@supports ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px))) {
    .loading-overlay {
        background: var(--glass-strong);
        -webkit-backdrop-filter: blur(var(--glass-blur-strong)) saturate(130%);
        backdrop-filter: blur(var(--glass-blur-strong)) saturate(130%);
    }
}

.loading-overlay.hidden {
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
}
```

Keep the wallet pulse, but replace its timing with `var(--motion-slow) var(--ease-standard)` and let the shared reduced-motion rule disable it.

- [ ] **Step 5: Dispatch one page-ready event when the page loads**

Replace `loading.js` with:

```javascript
window.addEventListener('load', () => {
    document.documentElement.classList.add('page-ready');
    document.getElementById('loadingOverlay')?.classList.add('hidden');
    window.dispatchEvent(new CustomEvent('rendo:page-ready'));
}, { once: true });
```

- [ ] **Step 6: Run focused tests and commit**

```powershell
.\mvnw.cmd -Dtest=LiquidGlassMarkupTest test
git add src/main/resources/templates/index.html src/main/resources/templates/fragments/loading.html src/main/resources/static/css/base.css src/main/resources/static/css/loading.css src/main/resources/static/js/loading.js src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java
git commit -m "feat(ui): coordinate glass landing and loading reveal"
```

Expected: tests PASS and commit succeeds.

### Task 5: Migrate authentication to theme-aware glass

**Files:**
- Modify: `src/main/resources/templates/login.html:24-30,298-301`
- Modify: `src/main/resources/static/css/login.css:14-105,169-241,336-414,608-700`
- Preserve: `src/main/resources/static/js/login.js`
- Test: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`

- [ ] **Step 1: Add auth DOM-preservation assertions before CSS changes**

Extend `contratosFuncionaisContinuamPresentes()` with:

```java
        assertTrue(login.contains("id=\"verificacao-form\""));
        assertTrue(login.contains("id=\"btn-cadastro\""));
        assertTrue(login.contains("id=\"btn-login\""));
        assertTrue(login.contains("name=\"username\""));
        assertTrue(login.contains("name=\"password\""));
```

- [ ] **Step 2: Remove fixed neutral token overrides from `.login-page`**

Keep only auth-specific geometry and brand-panel variables:

```css
.login-page {
    --auth-panel: rgba(36, 36, 58, 0.88);
    --auth-panel-alt: rgba(48, 48, 74, 0.92);
    --auth-panel-w: 45%;
    --auth-form-w: 55%;
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    position: relative;
}
```

Do not redeclare `--background`, `--surface`, `--border`, `--text-primary` or `--text-secondary`; they must come from the active theme.

- [ ] **Step 3: Convert the auth shell and form surfaces**

Set `.login-container` to `background: var(--glass-strong)`, `border-color: var(--glass-border-highlight)`, `border-radius: 28px`, shared shadow, and `backdrop-filter` via its `glass-surface-strong` class. Make forms transparent and use `var(--glass-standard)` for inputs/social buttons. Keep all existing widths, positions and slide transforms.

Use this theme-specific brand-panel adjustment:

```css
:root[data-theme="light"] .login-page {
    --auth-panel: rgba(45, 45, 70, 0.9);
    --auth-panel-alt: rgba(57, 57, 86, 0.94);
}

.toggle {
    background:
        linear-gradient(145deg, rgba(255,255,255,0.07), transparent 42%),
        var(--auth-panel);
    box-shadow: inset 0 1px 0 rgba(255,255,255,0.1);
}
```

- [ ] **Step 4: Refine interactions without changing login.js**

Use `--motion-fast`, `--motion-medium` and `--ease-emphasized` on submit buttons, password toggles, inputs and `.form-container`. Keep the existing `.active`, `.hidden`, `.valido`, `.invalido`, `.sign-in`, `.sign-up`, `.toggle-left` and `.toggle-right` selectors unchanged so no login.js edit is necessary.

- [ ] **Step 5: Preserve the narrow-layout contract**

Inside the existing `@media (max-width: 700px)`, keep the compact top band, immediate state swap and hidden decorative widgets. Reduce container radius to 22px, padding to 14px and blur to the shared mobile values; do not restore horizontal sliding on mobile.

- [ ] **Step 6: Run auth and markup tests**

```powershell
.\mvnw.cmd -Dtest=LiquidGlassMarkupTest,UsuarioAuthIntegrationTest test
```

Expected: both test classes PASS; no `login.js` functional changes are required.

- [ ] **Step 7: Commit authentication migration**

```powershell
git add src/main/resources/templates/login.html src/main/resources/static/css/login.css src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java
git commit -m "feat(auth): apply theme-aware liquid glass"
```

### Task 6: Migrate dashboard structure and dynamic surfaces

**Files:**
- Modify: `src/main/resources/templates/dashboard.html:26-360`
- Modify: `src/main/resources/static/css/dashboard.css:1-405`
- Modify: `src/main/resources/static/js/dashboard.js:691-695`
- Test: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`
- Test: `src/test/java/com/curso/gestaoinvestimentos/ui/DashboardAcoesMarkupTest.java`

- [ ] **Step 1: Expand dashboard contract assertions**

Add exact assertions for IDs used by navigation, graph and modal:

```java
        assertTrue(dashboard.contains("id=\"refreshAll\""));
        assertTrue(dashboard.contains("id=\"rendimentoChart\""));
        assertTrue(dashboard.contains("id=\"periodoPills\""));
        assertTrue(dashboard.contains("id=\"menuToggle\""));
        assertTrue(dashboard.contains("id=\"modal\""));
        assertTrue(dashboard.contains("id=\"toasts\""));
```

Also add a `dashboardScript()` helper and modal lifecycle assertions:

```java
    private String dashboardScript() throws IOException {
        return Files.readString(RESOURCES.resolve("static/js/dashboard.js"));
    }

    @Test
    void modalTemEntradaESaidaAnimadasSemAlterarSeuId() throws IOException {
        String script = dashboardScript();
        assertTrue(script.contains("classList.add(\"open\")"));
        assertTrue(script.contains("transitionend"));
        assertTrue(script.contains("classList.add(\"hidden\")"));
    }
```

Run `\.\mvnw.cmd -Dtest=LiquidGlassMarkupTest#modalTemEntradaESaidaAnimadasSemAlterarSeuId test` and confirm it fails before changing `dashboard.js`.

- [ ] **Step 2: Apply depth classes without changing functional selectors**

Use these class additions:

```html
<div class="app glass-stage">
<aside class="sidebar glass-surface glass-surface-strong" id="sidebar" data-motion>
<header class="topbar glass-surface glass-surface-subtle" data-motion>
<div class="stat-card glass-surface glass-interactive">
<div class="panel glass-surface">
<div class="rendo-modal-backdrop hidden" id="modal">
    <div class="rendo-modal glass-surface glass-surface-strong">
```

Apply `glass-interactive` only to cards/buttons that already have an action. Static financial tiles may receive border/reflex changes but SHALL NOT move on hover.

- [ ] **Step 3: Recompose the dashboard shell in CSS**

Change `.app` to a grid with outer padding and a gap, make sidebar rounded/sticky inside the viewport, make topbar a padded rounded surface, and leave `.main` min-width zero. Use:

```css
.app {
    min-height: 100vh;
    display: grid;
    grid-template-columns: 248px minmax(0, 1fr);
    gap: var(--space-5);
    padding: var(--space-4);
}

.sidebar {
    position: sticky;
    top: var(--space-4);
    height: calc(100vh - (var(--space-4) * 2));
    border-radius: 22px;
    padding: var(--space-6) var(--space-4);
}

.main { padding: 0 var(--space-4) var(--space-6) 0; min-width: 0; }

.topbar {
    position: sticky;
    top: var(--space-4);
    z-index: 20;
    align-items: center;
    padding: 14px 18px;
    border-radius: 18px;
    margin-bottom: var(--space-5);
}
```

At `max-width: 860px`, preserve the existing off-canvas sidebar behavior, remove outer grid padding on the sidebar track and keep the hamburger usable.

- [ ] **Step 4: Convert visual regions to token-driven glass**

For `.stat-card`, `.panel`, `.card`, `.chart-card`, `.pl-panel`, `.sidebar-ibov`, `.rendo-modal`, `.toast` and SweetAlert overrides, replace opaque `var(--surface)` backgrounds with the appropriate `--glass-*-solid` fallback and `--glass-*` value under `@supports`. Use `var(--glass-border)`, `var(--glass-inner-highlight)` and `var(--glass-shadow)` consistently.

Keep table rows stationary:

```css
.table tbody tr { transition: background-color var(--motion-fast) ease; }
.table tbody tr:hover { background-color: color-mix(in srgb, var(--brand) 6%, transparent); }
```

Do not apply `transform` to `tr`, `td`, `.mini-row` or non-interactive stat tiles.

- [ ] **Step 5: Refine modal and active navigation motion**

Use backdrop blur only when supported, render the active nav marker with `::before` using brand color, and use these modal states:

```css
.rendo-modal-backdrop {
    opacity: 0;
    visibility: hidden;
    transition: opacity 220ms var(--ease-standard), visibility 220ms linear;
}
.rendo-modal-backdrop:not(.hidden) { visibility: visible; }
.rendo-modal-backdrop.open { opacity: 1; }
.rendo-modal {
    transform: translateY(10px) scale(0.97);
    opacity: 0;
    transition: transform 220ms var(--ease-emphasized), opacity 180ms ease;
}
.rendo-modal-backdrop.open .rendo-modal {
    transform: translateY(0) scale(1);
    opacity: 1;
}
```

Keep `.nav-item.active` and `.rendo-modal-backdrop.hidden` as the functional selectors.

- [ ] **Step 6: Add the symmetric modal lifecycle and only necessary dynamic classes**

Replace the two modal functions with:

```javascript
let modalCloseTimer;

function abrirModal(html) {
    const modal = $("#modal");
    window.clearTimeout(modalCloseTimer);
    $("#modalContent").innerHTML = html;
    modal.classList.remove("hidden");
    requestAnimationFrame(() => modal.classList.add("open"));
}

function fecharModal() {
    const modal = $("#modal");
    if (modal.classList.contains("hidden")) return;
    modal.classList.remove("open");
    const finish = () => modal.classList.add("hidden");
    modal.addEventListener("transitionend", finish, { once: true });
    modalCloseTimer = window.setTimeout(finish, 260);
}
```

The timeout is a fallback for reduced motion or browsers that do not emit `transitionend`; calling `finish` twice is idempotent.

Search before editing:

```powershell
rg -n "class=\\\"(card|panel|btn|mini-row|toast)" src/main/resources/static/js/dashboard.js
```

If a dynamic interactive card needs elevation, add only `glass-interactive` to its existing class attribute. Do not change fetch calls, IDs, event delegation, calculations, HTML field names or API paths.

- [ ] **Step 7: Run dashboard-focused tests**

```powershell
.\mvnw.cmd -Dtest=LiquidGlassMarkupTest,DashboardAcoesMarkupTest,OperacaoIntegrationTest test
```

Expected: all selected tests PASS.

- [ ] **Step 8: Commit dashboard migration**

```powershell
git add src/main/resources/templates/dashboard.html src/main/resources/static/css/dashboard.css src/main/resources/static/js/dashboard.js src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java
git commit -m "feat(dashboard): add layered liquid glass interface"
```

### Task 7: Visual, accessibility and traceability verification

**Files:**
- Modify: `openspec/changes/liquid-glass-interface/tasks.md`
- Regenerate ignored output: `graphify-out/`

- [ ] **Step 1: Run the full automated suite**

```powershell
.\mvnw.cmd test
```

Expected: `BUILD SUCCESS`, zero failures and zero errors.

- [ ] **Step 2: Run static consistency checks**

```powershell
git diff --check
rg -n "backdrop-filter|prefers-reduced-motion|@supports" src/main/resources/static/css/glass.css src/main/resources/static/css/login.css src/main/resources/static/css/dashboard.css src/main/resources/static/css/loading.css
rg -n "rendo:page-ready|IntersectionObserver|Math\.min\(index, 6\)" src/main/resources/static/js/loading.js src/main/resources/static/js/motion.js
```

Expected: no whitespace errors; CSS shows fallback/support/reduced-motion rules; JS shows the shared ready event, observer and bounded stagger.

- [ ] **Step 3: Start the application for browser verification**

Run in a dedicated terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

Expected: application starts on `http://localhost:8080` with the dev/H2 profile.

- [ ] **Step 4: Verify the four visual matrices**

Use browser automation/screenshots to inspect:

1. Dark desktop: landing, login/cadastro/verificação and every dashboard view/modal.
2. Light desktop: the same screens with equivalent depth and readable focus.
3. Dark mobile at 390×844: compact auth band, off-canvas dashboard sidebar, no horizontal overflow.
4. Light mobile at 390×844: same behavior and equivalent finish.

For each matrix, verify console has no new errors, keyboard focus is visible, buttons remain discoverable without hover, tables stay aligned, modals retain all fields, and loading appears only under the existing conditions.

- [ ] **Step 5: Verify reduced motion and no-blur fallback**

Emulate `prefers-reduced-motion: reduce`; confirm atmosphere/stagger/elevation stop while feedback remains. Disable `backdrop-filter` in DevTools or force the `@supports` fallback; confirm opaque surfaces preserve hierarchy and contrast.

- [ ] **Step 6: Validate and complete OpenSpec tasks**

Mark each OpenSpec checkbox only after its evidence exists, then run:

```powershell
openspec validate liquid-glass-interface --strict --no-interactive
openspec status --change liquid-glass-interface
```

Expected: change valid and `4/4 artifacts complete`; implementation checkboxes reflect actual completed work.

- [ ] **Step 7: Update and query Graphify**

```powershell
graphify update .
graphify query "Quais telas e componentes dependem agora do sistema liquid glass compartilhado?" --budget 3000
graphify affected "motion.js" --depth 3
```

Expected: graph rebuild succeeds; query includes landing, login, dashboard, loading, tokens/theme and the OpenSpec change. Investigate any unexpected dependency before completion.

- [ ] **Step 8: Commit verification metadata**

```powershell
git add openspec/changes/liquid-glass-interface/tasks.md
git commit -m "docs: record liquid glass verification"
git status --short
```

Expected: commit succeeds and the working tree is clean. Do not archive the OpenSpec change until the user accepts the implemented result.
