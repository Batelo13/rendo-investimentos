## Why

A interface do dashboard funcionava, mas com aparência genérica de "template de dashboard gerado por IA": card dentro de card (indicadores + painel "Resultado da carteira" separado fazendo o mesmo trabalho), glow decorativo em valores financeiros, sidebar com item ativo em formato de "botão bolha", gráfico de rendimento raso e sem interatividade, nenhum ícone consistente (só emoji). O usuário pediu um redesign completo rumo a uma estética "fintech/investment dashboard premium", preservando 100% do comportamento/dados/endpoints existentes.

## What Changes

- **Design tokens** (`tokens.css`) trocados para a paleta pedida (`--background`, `--surface`, `--surface-elevated`, `--border`, `--text-primary`, `--text-secondary`, `--brand`, `--success`, `--danger`, `--warning`), com escalas novas de radius e spacing. Lucro/prejuízo passa a usar `--success` em vez de `--brand` (mint deixa de representar lucro indiscriminadamente).
- **Sidebar** mais compacta e densa, com ícone por item de navegação (SVG inline estilo Lucide, vendorizado — sem dependência nova) e estado ativo por barra lateral + peso de fonte, não mais um "botão bolha".
- Indicadores da Visão Geral e o antigo painel "Resultado da carteira" **fundidos numa única grade** de 6 tiles (Saldo disponível, Saldo inicial, Posições, Corretoras, Resultado não realizado, Resultado realizado) — elimina o padrão card-dentro-de-card.
- **Gráfico "Evolução patrimonial"**: mais alto, com grid horizontal discreto, preenchimento de área sutil, tooltip on-hover, e **pills de período (1D/1M/3M/6M/1A/Tudo) que filtram no cliente** os pontos já carregados por data — sem endpoint novo, sem inventar dado que o backend não fornece.
- **Mini-lista "Minhas posições"** (Visão Geral) vira mini-tabela em grid com logo + ticker + nome da empresa, corretora, quantidade, preço médio, resultado e valor da posição — dados já disponíveis via lookup em `state.acoes` (mesmo padrão do helper `moedaPorTicker` já existente).
- Todo emoji funcional (☰ ✕ ⟳ → ← ⤢ ✔ 📍 🇺🇸 🇧🇷) substituído por ícones SVG inline consistentes; flags de país viram badge de texto.
- Botões formalizados em 4 níveis (Primary/Secondary/Ghost/Danger) — classes já usadas dinamicamente pelo JS (`.btn-buy`, `.btn-sell`, `.btn-icon`) preservadas.
- `login.css`/`loading.css` migrados pros novos tokens (sem redesign estrutural do login, fora do escopo desta change).

## Capabilities

### New Capabilities
- `redesign-dashboard-visual`: nova linguagem visual do dashboard (tokens, sidebar, indicadores unificados) e as duas capacidades interativas novas do gráfico de evolução patrimonial (filtro de período, tooltip on-hover).

### Modified Capabilities
(nenhuma — nenhum endpoint, DTO, model ou regra de negócio muda; os dados exibidos são os mesmos, só reorganizados/redesenhados)

## Impact

- `static/css/tokens.css`, `static/css/dashboard.css` — reescrita.
- `static/css/login.css`, `static/css/loading.css` — rename de variáveis (sem redesign estrutural).
- `templates/dashboard.html` — sidebar, topbar, seção Visão Geral.
- `static/js/dashboard.js` — helpers de ícone, `renderVisaoGeral()`, `renderRendimentoChart()` (+ filtro de período e tooltip), troca pontual de emoji nas demais views.
- Nenhum arquivo `.java`, endpoint, DTO ou `application.properties` tocado.
