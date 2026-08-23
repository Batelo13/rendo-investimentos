## Context

Ver proposal.md - Why. Stack: Thymeleaf + Bootstrap 5.3.3 (só CSS, sem JS) + JS vanilla, zero build step, zero framework de componentes. `dashboard.css` já usava variáveis CSS (`--rendo-color-*`) e já tinha um precedente de renomear classe pra evitar colisão com componente do Bootstrap (`.modal` → `.rendo-modal`, documentado no próprio CSS).

## Goals / Non-Goals

**Goals:**
- Composição nova (não só cor): eliminar card-dentro-de-card, glow decorativo, "botão bolha" de nav ativo.
- Ícones consistentes sem adicionar dependência de build (o projeto não tem bundler).
- Gráfico como protagonista real: filtro de período e tooltip usando só dados já carregados.
- Preservar toda classe CSS referenciada dinamicamente por `dashboard.js` (`.btn-buy`, `.btn-sell`, `.btn-icon`, `.btn`, `.btn-primary`, `.btn-sm`).

**Non-Goals:**
- Redesign estrutural do login (painel deslizante, blobs) — sistema visual próprio já polido, fora do pedido ("principalmente dashboard/Visão Geral").
- Mudar colunas/comportamento da página completa "Minhas posições" — só a mini-lista da Visão Geral foi redesenhada com colunas novas.
- Endpoint de período no backend — o filtro é 100% client-side sobre dados já retornados por `GET /carteiras/me/rendimento-historico`.

## Decisions

- **Ícones Lucide como SVG inline vendorizado**, não a lib npm inteira: só ~12 ícones são usados, e adicionar `lucide` como dependência exigiria um passo de build que o projeto deliberadamente não tem. Paths copiados diretamente do set Lucide (MIT), consistente com o estilo já usado nos ícones sol/lua existentes.
- **Tokens sem o prefixo `--rendo-color-`**: o pedido do usuário listava nomes exatos (`--background`, `--surface`, etc.). Mantido apenas em infraestrutura não solicitada explicitamente (`--rendo-font-body`/`--rendo-font-logo`) pra não colidir com nomes genéricos demais.
- **Filtro de período 100% client-side**: os pontos completos já vêm em uma única chamada (`rendimento-historico`); filtrar por `timestamp >= agora - N dias` no array já carregado evita endpoint novo e mantém "Tudo" como o comportamento de hoje.
- **Tooltip sem lib de gráfico**: mesmo padrão já usado (SVG construído na mão) — um array `pontosChartAtual` guarda a projeção de cada ponto no espaço do viewBox a cada render, e um listener de `mousemove` (ligado uma vez, não recriado a cada render) acha o ponto mais próximo e posiciona um `<div>` tooltip absoluto sobre o wrap.
- **Lucro/prejuízo usa `--success`, nunca `--brand`**: pedido explícito do usuário ("não utilize mint da marca indiscriminadamente pra representar lucro") — muda o valor de `.pl-pos` globalmente (afeta também Operações/Posições, não só Visão Geral), decisão consciente de consistência.
- **`.stat-value.pos/.neg` não foi criado**: os dois tiles de resultado continuam usando o `<span class="pl pl-pos/neg/zero">` que `fmtResultado()` já injeta — evita uma segunda forma de colorir o mesmo dado.

## Risks / Trade-offs

- [Ícones SVG inline duplicados entre `dashboard.html` (estático) e `dashboard.js` (dinâmico, via helper `icone()`)] → aceito; é o mesmo padrão que `mercadoTag()`/`acaoLogoHTML()` já usavam antes desta change, não uma inconsistência nova.
- [Responsividade (1920/1440/1366/1024/tablet/mobile) verificada por inspeção do CSS, não visualmente em todos os breakpoints — a ferramenta de redimensionar janela do ambiente de teste não surtiu efeito na sessão] → mitigado: breakpoints usam o mesmo padrão `@media (max-width: Npx)` já comprovado no arquivo antes da change; recomendado um teste manual rápido do usuário redimensionando a janela real.

## Migration Plan

Mudança é 100% de apresentação (CSS/HTML/JS estático); nenhuma migração de schema ou dado. Rollback é reverter o commit/branch.
