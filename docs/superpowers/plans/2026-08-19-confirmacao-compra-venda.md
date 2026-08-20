# Confirmação de Compra/Venda com SweetAlert2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Adicionar uma caixa de confirmação (SweetAlert2) antes de executar uma compra ou venda de ação, reaproveitando o resumo já calculado no modal, sem tocar nos toasts existentes.

**Architecture:** Vendorizar `sweetalert2.all.min.js` localmente em `static/js/vendor/` (sem CDN, sem build step), estilizar a caixa com os tokens `--rendo-color-*` já existentes (acompanha o tema claro/escuro de graça, via CSS var), e inserir um `await Swal.fire(...)` dentro do `submitOperacao()` existente antes da chamada à API.

**Tech Stack:** JS vanilla, Thymeleaf, SweetAlert2 (vendorizado), CSS custom properties.

---

### Task 1: Vendorizar o SweetAlert2

**Files:**
- Create: `src/main/resources/static/js/vendor/sweetalert2.all.min.js`
- Modify: `src/main/resources/templates/dashboard.html:257`
- Modify: `.gitignore`

- [x] **Step 1: Copiar o arquivo do node_modules**

Run:
```bash
mkdir -p src/main/resources/static/js/vendor
cp node_modules/sweetalert2/dist/sweetalert2.all.min.js src/main/resources/static/js/vendor/sweetalert2.all.min.js
```

Expected: arquivo `src/main/resources/static/js/vendor/sweetalert2.all.min.js` existe (mesma versão `^11.26.25` já instalada no `package.json`).

- [x] **Step 2: Adicionar o script no template**

Em `src/main/resources/templates/dashboard.html`, os scripts atuais (linhas 257-259) são:

```html
<script th:src="@{/js/loading.js}"></script>
<script th:src="@{/js/theme.js}"></script>
<script th:src="@{/js/dashboard.js}"></script>
```

Trocar por (o vendor precisa carregar antes de `dashboard.js`, que vai usar `Swal`):

```html
<script th:src="@{/js/loading.js}"></script>
<script th:src="@{/js/theme.js}"></script>
<script th:src="@{/js/vendor/sweetalert2.all.min.js}"></script>
<script th:src="@{/js/dashboard.js}"></script>
```

- [x] **Step 3: Ignorar node_modules no git**

Em `.gitignore`, adicionar uma seção nova no final do arquivo (depois de `### Segredos locais (nunca commitar) ###` / `.env`):

```
### Node (sweetalert2 vendorizado a partir daqui, node_modules nao e commitado) ###
node_modules/
```

`package.json`/`package-lock.json` continuam versionados (documentam de onde o arquivo vendorizado veio); só `node_modules/` fica de fora.

- [x] **Step 4: Commit**

```bash
git add src/main/resources/static/js/vendor/sweetalert2.all.min.js src/main/resources/templates/dashboard.html .gitignore package.json package-lock.json
git commit -m "feat(frontend): vendoriza SweetAlert2 localmente"
```

---

### Task 2: Estilizar a caixa com os tokens Rendo

**Files:**
- Modify: `src/main/resources/static/css/dashboard.css`

- [x] **Step 1: Add the CSS rule**

Em `src/main/resources/static/css/dashboard.css`, a regra `.toast.info` (linha 216 hoje) é a última do bloco de toasts:

```css
.toast.info { border-left: 3px solid var(--rendo-color-text-muted); }
```

Adicionar logo em seguida um bloco novo pra estilizar o SweetAlert2:

```css
/* ------------------------ SweetAlert2 (confirmacao) ---------------------- */
.swal2-popup { background: var(--rendo-color-surface); color: var(--rendo-color-text); border-radius: var(--rendo-radius-md); }
.swal2-title { color: var(--rendo-color-text); }
.swal2-html-container { color: var(--rendo-color-text-muted); }
.swal-resumo .op-resumo-row { margin-top: 8px; }
.swal-resumo .op-resumo-row:first-child { margin-top: 0; }
.swal2-confirm { background: var(--rendo-color-accent) !important; color: var(--rendo-color-bg) !important; box-shadow: none !important; }
.swal2-cancel { background: transparent !important; color: var(--rendo-color-text-muted) !important; border: 1px solid var(--rendo-color-border) !important; box-shadow: none !important; }
```

- [x] **Step 2: Commit**

```bash
git add src/main/resources/static/css/dashboard.css
git commit -m "feat(frontend): estiliza caixa do SweetAlert2 com os tokens Rendo"
```

Nenhum teste automatizado pra este passo isolado — o visual só é exercitado de verdade quando a caixa é aberta (Task 3), verificado manualmente no navegador (Task 4).

---

### Task 3: Adicionar a confirmação em `submitOperacao()`

**Files:**
- Modify: `src/main/resources/static/js/dashboard.js:524-553`

- [x] **Step 1: Inserir a confirmação antes da chamada à API**

Em `src/main/resources/static/js/dashboard.js`, a função `submitOperacao` hoje é:

```javascript
async function submitOperacao(e, acao) {
    e.preventDefault();
    const form = e.target;
    const tipo = form.dataset.tipo;
    const btn = $("#btnOperacao");

    const acaoId = tipo === "COMPRA" ? acao.id : Number(form.dataset.acaoId);
    const corretoraId = tipo === "COMPRA" ? Number(form.corretoraId.value) : Number(form.dataset.corretoraId);
    const quantidade = Number(form.quantidade.value);
    const precoUnitario = Number(form.precoUnitario.value);

    if (tipo === "COMPRA" && !corretoraId) return toast("Validação", "Selecione uma corretora.", "err");
    if (!quantidade || quantidade <= 0) return toast("Validação", "Informe uma quantidade válida.", "err");
    if (!precoUnitario || precoUnitario <= 0) return toast("Validação", "Informe um preço unitário válido.", "err");

    setLoading(btn, true, tipo === "COMPRA" ? "Comprando…" : "Vendendo…");
    try {
        await api("/operacoes", {
            method: "POST",
            body: JSON.stringify({ acaoId, corretoraId, tipo, quantidade, precoUnitario }),
        });
        toast(tipo === "COMPRA" ? "Compra registrada" : "Venda registrada", `${quantidade} un. de ${acao.ticker}`, "ok");
        fecharModal();
        await carregarTudo();
    } catch (err) {
        toast(tipo === "COMPRA" ? "Falha na compra" : "Falha na venda", err.message, "err");
    } finally {
        setLoading(btn, false, tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda");
    }
}
```

Substituir por (adiciona o bloco `Swal.fire` entre as validações e o `setLoading`, e um `return` antecipado se o usuário cancelar):

```javascript
async function submitOperacao(e, acao) {
    e.preventDefault();
    const form = e.target;
    const tipo = form.dataset.tipo;
    const btn = $("#btnOperacao");

    const acaoId = tipo === "COMPRA" ? acao.id : Number(form.dataset.acaoId);
    const corretoraId = tipo === "COMPRA" ? Number(form.corretoraId.value) : Number(form.dataset.corretoraId);
    const quantidade = Number(form.quantidade.value);
    const precoUnitario = Number(form.precoUnitario.value);

    if (tipo === "COMPRA" && !corretoraId) return toast("Validação", "Selecione uma corretora.", "err");
    if (!quantidade || quantidade <= 0) return toast("Validação", "Informe uma quantidade válida.", "err");
    if (!precoUnitario || precoUnitario <= 0) return toast("Validação", "Informe um preço unitário válido.", "err");

    const resumoHtml = $("#opResumo").innerHTML;
    const { isConfirmed } = await Swal.fire({
        title: tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda",
        html: `<div class="swal-resumo">${resumoHtml}</div>`,
        showCancelButton: true,
        confirmButtonText: tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda",
        cancelButtonText: "Cancelar",
    });
    if (!isConfirmed) return;

    setLoading(btn, true, tipo === "COMPRA" ? "Comprando…" : "Vendendo…");
    try {
        await api("/operacoes", {
            method: "POST",
            body: JSON.stringify({ acaoId, corretoraId, tipo, quantidade, precoUnitario }),
        });
        toast(tipo === "COMPRA" ? "Compra registrada" : "Venda registrada", `${quantidade} un. de ${acao.ticker}`, "ok");
        fecharModal();
        await carregarTudo();
    } catch (err) {
        toast(tipo === "COMPRA" ? "Falha na compra" : "Falha na venda", err.message, "err");
    } finally {
        setLoading(btn, false, tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda");
    }
}
```

- [x] **Step 2: Compilar/checar sintaxe**

Run: `node --check src/main/resources/static/js/dashboard.js`
Expected: sem saída (sintaxe válida).

- [x] **Step 3: Commit**

```bash
git add src/main/resources/static/js/dashboard.js
git commit -m "feat(frontend): confirma compra/venda com SweetAlert2 antes de enviar"
```

Nenhum teste automatizado pra este passo — o projeto não tem framework de teste JS (mesmo padrão já usado nas outras features de frontend, ex. conversão de câmbio). Verificação é manual no navegador (Task 4).

---

### Task 4: Verificação manual no navegador

Cobre o cenário completo: abrir o modal, ver a caixa de confirmação com o resumo certo, cancelar sem efeito colateral, confirmar e ver a operação registrada.

- [x] **Step 1: Start the app**

Run: `.\mvnw.cmd spring-boot:run`

(Se o app já estava rodando de uma sessão anterior, mate o processo antigo primeiro — edições em `static/`/`templates/` não hot-reload em `spring-boot:run` já em execução.)

- [x] **Step 2: Comprar uma ação e ver a caixa de confirmação**

Login no dashboard, ir em "Ações", clicar "Comprar" numa ação, preencher corretora/quantidade/preço. Clicar "Confirmar compra". Confirmar: abre a caixa do SweetAlert2 com título "Confirmar compra" e o mesmo resumo que já aparecia no modal (valor total, resultado estimado, conversão em R$ se for ação EUA).

- [x] **Step 3: Cancelar não deve ter efeito colateral**

Clicar "Cancelar" na caixa. Confirmar: caixa fecha, modal de compra continua aberto com os campos preenchidos, nenhum toast aparece, saldo não muda.

- [x] **Step 4: Confirmar deve registrar a operação normalmente**

Clicar "Confirmar compra" de novo, e agora "Confirmar compra" na caixa do SweetAlert2. Confirmar: operação é registrada (toast de sucesso), modal fecha, saldo e posições atualizam — mesmo comportamento de antes desta feature, só com o passo extra de confirmação.

- [x] **Step 5: Repetir para venda**

Vender uma ação que já tem posição. Confirmar: mesmo fluxo (caixa com título "Confirmar venda", cancelar não muda nada, confirmar registra a venda).

- [x] **Step 6: Conferir o tema claro/escuro**

Alternar o tema (botão de tema) e repetir o Step 2 em cada tema. Confirmar: a caixa acompanha as cores do tema (fundo, texto, botões), sem contraste quebrado (texto ilegível, botão sem cor de fundo, etc.).

- [x] **Step 7: Checar o console do navegador**

Sem erros novos no console (só ruído de extensão do Chrome, se houver, como já visto em sessões anteriores).
