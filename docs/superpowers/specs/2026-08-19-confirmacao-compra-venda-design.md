# Confirmação de Compra/Venda com SweetAlert2 — Design

Data: 2026-08-19
Branch prevista: nova branch a partir de `main` (depois que a branch `feature-conversao-cambio-eua` for mergeada)

## Contexto

O modal de compra/venda de ações já mostra um resumo ao vivo (valor total, resultado estimado, conversão em R$ para ações EUA) conforme o usuário digita, mas o botão "Confirmar compra"/"Confirmar venda" envia a operação direto para a API — não existe nenhum passo de confirmação entre o clique e a operação afetar o saldo. `sweetalert2` já está instalado (`package.json`, `^11.26.25`) mas não é usado em lugar nenhum do código ainda.

Os toasts de sucesso/erro (`toast()` em `dashboard.js`) já existem, feitos à mão, e ficam como estão — não fazem parte desta change.

## Escopo

- Caixa de confirmação (SweetAlert2) antes de executar uma compra ou venda de ação — a única ação que mexe no saldo da carteira. Cadastro de ação/corretora continua direto, sem confirmação.
- Vendorizar a lib localmente (`static/js/vendor/`), sem depender de CDN.
- Estilizar a caixa com os tokens de cor já existentes (`--rendo-color-*`), acompanhando o tema claro/escuro.

Fora de escopo (decisão explícita, não construído agora):

- Confirmação para cadastro de ação/corretora — só compra/venda mexe em dinheiro.
- Substituir os toasts atuais por SweetAlert2 — o `toast()` existente continua sendo o único canal de sucesso/erro.
- Qualquer teste automatizado de frontend — o projeto não tem framework de teste JS (mesmo padrão já usado nas outras features de frontend); verificação é manual no navegador.

## Mudanças em componentes existentes

### `dashboard.html`

Novo `<script>` local apontando pro arquivo vendorizado, carregado antes de `dashboard.js`:

```html
<script src="/js/vendor/sweetalert2.all.min.js"></script>
```

### `static/js/vendor/sweetalert2.all.min.js` (novo, vendorizado)

Cópia direta de `node_modules/sweetalert2/dist/sweetalert2.all.min.js` (versão `^11.26.25`, a mesma já instalada via npm) — sem build step, sem bundler, mesmo padrão "estático servido direto" que o resto do projeto já usa.

### `dashboard.js` — `submitOperacao()`

Único ponto de mudança. Depois das validações de campo (que continuam iguais) e antes da chamada à API, insere um `await Swal.fire(...)` reaproveitando o mesmo HTML que `ligarResumoOperacao()`/`calcResumo()` já monta em `#opResumo` (mesmas funções `fmtMoeda`/`fmtConvertido`/`fmtResultado`, nada novo):

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

Cancelar (`isConfirmed === false`) simplesmente retorna — o modal continua aberto com os dados preenchidos, nada é enviado, nenhum toast aparece.

### `dashboard.css`

Novo bloco estilizando as classes que o SweetAlert2 renderiza (`.swal2-popup`, `.swal2-title`, `.swal2-html-container`, `.swal2-confirm`, `.swal2-cancel`) com os tokens já existentes:

```css
.swal2-popup {
    background: var(--rendo-color-surface);
    color: var(--rendo-color-text);
    border-radius: var(--rendo-radius-md);
}
.swal2-confirm {
    background: var(--rendo-color-accent) !important;
    color: var(--rendo-color-bg) !important;
}
.swal2-cancel {
    background: transparent !important;
    color: var(--rendo-color-text-muted) !important;
    border: 1px solid var(--rendo-color-border) !important;
}
```

Como são `var()`, a troca de tema (`data-theme="light"` na `<html>`, feita por `theme.js`) já resolve as cores automaticamente — sem nenhum JS de tema adicional. `.swal-resumo` reaproveita o CSS que `.op-resumo-row`/`.valor-convertido` já têm.

## Erros e validações

Inalterado: falhas na chamada `/operacoes` continuam usando o `toast()` existente, exatamente como hoje. O SweetAlert2 entra só no passo de confirmação, nunca para reportar erro.

## Testes

Sem framework de teste JS no projeto — verificação manual no navegador, mesmo padrão das outras features de frontend:

- Comprar uma ação: preencher o formulário, clicar "Confirmar compra" abre a caixa do SweetAlert2 com o mesmo resumo (valor total, resultado estimado, conversão em R$ se for EUA).
- Cancelar na caixa: modal continua aberto, campos preenchidos, saldo inalterado, nenhum toast.
- Confirmar na caixa: operação é registrada normalmente (toast de sucesso, modal fecha, saldo atualizado) — mesmo comportamento de hoje, só com o passo extra.
- Repetir o mesmo para venda.
- Alternar tema claro/escuro com a caixa aberta (ou abrir em cada tema): cores acompanham o tema, sem contraste quebrado.

## Self-Review

**Placeholder scan:** nenhum "TBD"/"TODO" — todas as decisões (escopo só compra/venda, vendorizar localmente, seguir tema, toasts inalterados) vieram das respostas do usuário durante o brainstorming.

**Consistência interna:** reaproveita o HTML/funções de formatação que `#opResumo` já monta — não duplica lógica de cálculo. Segue o mesmo padrão "sem build step" que o resto do projeto (`static/` servido direto).

**Fora de escopo, e por quê:** confirmação em cadastro de ação/corretora (não mexe em saldo, não pedido); substituir toasts (decisão explícita do usuário de manter o `toast()` atual); testes automatizados de frontend (projeto não tem esse framework, mesmo gap já aceito nas outras features).
