## Why

Uma "posição" hoje é sempre por **ação + corretora** (decisão de modelagem deliberada: diversificação entre corretoras já é representada por linhas separadas, não por múltiplas carteiras). Isso significa que se o usuário compra a mesma ação em duas corretoras diferentes, a Visão Geral do dashboard mostrava duas linhas separadas, cada uma com seu próprio "Preço médio acumulado" — nunca um preço médio único daquela ação somando todas as corretoras. O professor pediu explicitamente que o dashboard (Visão Geral) mostre o preço médio consolidado por ação, mantendo o detalhamento por corretora na tela "Minhas posições" (que já existe justamente para isso).

## What Changes

- A mini-lista "Minhas posições" da **Visão Geral** passa a agrupar por `acaoTicker`, somando quantidade e valor investido de todas as corretoras e calculando um preço médio ponderado único por ação (`somaValorInvestido / somaQuantidade`).
- Quando a ação está em mais de uma corretora, o selo de corretora mostra a contagem (`×N` / "N corretoras") em vez do nome de uma corretora específica.
- A ação rápida "Vender" é removida da mini-lista (uma linha consolidada não mapeia para uma corretora específica, então vender exige ir para "Minhas posições", que já tem o botão por linha).
- A tabela completa **"Minhas posições"** (rota/aba dedicada) não muda: continua uma linha por ação+corretora, com "Preço médio acumulado" por posição — esse comportamento já é o propósito documentado da tela.
- Nenhuma mudança de backend: a agregação é feita inteiramente no client a partir de `state.posicoes`, já carregado por completo (mesmo padrão do filtro textual/estruturado existente).

## Capabilities

### New Capabilities
- `preco-medio-consolidado-por-acao`: agregação client-side do preço médio por ação (todas as corretoras) na Visão Geral.

### Modified Capabilities
(nenhuma — a tela "Minhas posições" e sua spec `filtro-posicoes` não mudam de comportamento)

## Impact

- `src/main/resources/static/js/dashboard.js`: nova função `consolidarPosicoesPorAcao()`; `renderVisaoGeral()` passa a consolidar antes de renderizar a mini-lista; remoção do menu kebab/"Vender" da mini-lista.
- `src/main/resources/static/css/dashboard.css`: `.mini-row` perde a última coluna (26px, do botão kebab) nas 3 definições de grid (base + 2 breakpoints responsivos).
- Nenhum endpoint, DTO ou entidade de backend é alterado.
