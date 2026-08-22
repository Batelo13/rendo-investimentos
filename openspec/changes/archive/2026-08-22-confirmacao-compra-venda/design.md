## Context

O modal de compra/venda já monta um resumo ao vivo (`#opResumo`, atualizado por `ligarResumoOperacao()`/`calcResumo()`) conforme o usuário digita, mas `submitOperacao()` chamava `POST /operacoes` direto no clique do botão. `sweetalert2` (`^11.26.25`) já estava em `package.json` mas não era usado. Os toasts de sucesso/erro (`toast()` em `dashboard.js`) são feitos à mão e não fazem parte desta change. Ver proposal.md - Why.

## Goals / Non-Goals

**Goals:**
- Inserir um passo de confirmação (aceitar/cancelar) só para compra/venda, reaproveitando o HTML/funções de formatação que `#opResumo` já monta — sem duplicar lógica de cálculo.
- Vendorizar a lib localmente, sem CDN, sem build step — mesmo padrão "estático servido direto" já usado no projeto.
- Estilizar a caixa com os tokens `--rendo-color-*` já existentes, para acompanhar o tema claro/escuro automaticamente.

**Non-Goals:**
- Confirmação em cadastro de ação/corretora — não mexe em saldo.
- Substituir os toasts atuais por SweetAlert2 — `toast()` continua sendo o único canal de sucesso/erro.
- Teste automatizado de frontend — projeto não tem framework de teste JS; verificação é manual no navegador, mesmo padrão já aceito nas outras features de frontend.

## Decisions

- **Vendorizar em vez de CDN**: cópia direta de `node_modules/sweetalert2/dist/sweetalert2.all.min.js` para `static/js/vendor/sweetalert2.all.min.js`. Evita dependência de rede em runtime e mantém o padrão já usado pelo resto do projeto (Bootstrap via webjar, não CDN).
- **Reaproveitar `#opResumo` no corpo do `Swal.fire`**: em vez de remontar o resumo com nova lógica, o HTML de `$("#opResumo").innerHTML` é injetado direto na caixa (`<div class="swal-resumo">...</div>`). Alternativa considerada (recalcular o resumo dentro do handler de confirmação) foi descartada por duplicar `fmtMoeda`/`fmtConvertido`/`fmtResultado` sem necessidade.
- **Único ponto de mudança em `submitOperacao()`**: o `await Swal.fire(...)` entra depois das validações de campo e antes da chamada à API. Cancelar (`isConfirmed === false`) apenas retorna, sem side effects.
- **Estilização via CSS custom properties já existentes**: `.swal2-popup`/`.swal2-confirm`/`.swal2-cancel` mapeados para `--rendo-color-surface`/`--rendo-color-accent`/`--rendo-color-border` etc. Como são `var()`, a troca de tema (`data-theme="light"`, feita por `theme.js`) resolve as cores automaticamente, sem JS de tema adicional.

## Risks / Trade-offs

- [Lib vendorizada desatualiza silenciosamente, já que não há CDN nem lockfile automatizado para o arquivo estático] → mitigação: versão documentada no design (`^11.26.25`, mesma do `package.json`); atualização é manual, copiando de `node_modules` novamente quando necessário.
- [Confirmação vira atrito extra em fluxos de teste manual/demonstração] → aceito conscientemente: é a única ação que mexe em saldo, o atrito é o objetivo da mudança.

## Migration Plan

Mudança é aditiva e local ao frontend (um novo arquivo vendorizado + edições em `dashboard.html`/`dashboard.js`/`dashboard.css`); não há dado migrado nem endpoint alterado. Rollback é reverter o commit/branch — nenhuma migração de banco envolvida.
