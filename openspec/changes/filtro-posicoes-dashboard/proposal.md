## Why

A tela "Minhas posições" do dashboard já tem uma busca textual (`#filterPosicoes`) por ticker/corretora, mas quando o usuário tem posições em vários mercados, corretoras e com resultados mistos (lucro/prejuízo), não há como isolar rapidamente um subconjunto sem digitar texto exato. O usuário quer filtros estruturados para enxergar, por exemplo, só as posições com prejuízo, ou só as da carteira nos EUA, ou só de uma corretora específica.

## What Changes

- Adiciona controles de filtro por **Mercado** (BRASIL/EUA), **Corretora** e **Resultado** (lucro/prejuízo/neutro) na tela "Minhas posições" do dashboard, ao lado da busca textual existente.
- Os filtros combinam entre si e com a busca textual já existente (AND lógico): a tabela mostra apenas posições que satisfazem todos os critérios ativos.
- O filtro de Corretora é populado dinamicamente com as corretoras presentes nas posições do usuário (sem opção fixa/hardcoded).
- O filtro de Resultado classifica cada posição por `valorAtual - valorInvestido`: positivo (lucro), negativo (prejuízo) ou zero (neutro).
- Nenhuma chamada de rede nova: os filtros operam sobre `state.posicoes`, já carregado integralmente no client (mesmo padrão do filtro textual atual).
- Fora de escopo: filtro por "tipo de ativo" (ação/FII/ETF) — esse dado não existe no modelo `Acao` hoje; ficará para um change futuro que inclua a modelagem correspondente.

## Capabilities

### New Capabilities
- `filtro-posicoes`: filtros estruturados (mercado, corretora, resultado) combináveis na listagem de posições do dashboard.

### Modified Capabilities
(nenhuma — não há requisito de spec existente sendo alterado; `redesign-dashboard-visual` cobre layout geral, não o comportamento de filtragem)

## Impact

- `src/main/resources/templates/dashboard.html`: novos controles de filtro (selects) no painel de posições.
- `src/main/resources/static/js/dashboard.js`: lógica de `renderPosicoes()` estendida para aplicar os filtros combinados; popular dinamicamente o select de corretoras.
- `src/main/resources/static/css/*` (se existente): estilo dos novos controles, seguindo o padrão visual do `redesign-dashboard-visual`.
- Nenhum endpoint de backend, DTO ou entidade é alterado.
