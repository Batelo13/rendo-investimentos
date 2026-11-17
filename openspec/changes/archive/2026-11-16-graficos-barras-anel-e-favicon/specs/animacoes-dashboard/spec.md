## ADDED Requirements

### Requirement: Gráfico de barras por ação e anel de capital investido
A visão geral SHALL exibir uma barra por ação em carteira, com o valor atual em R$ (as maiores primeiro e a maior em destaque), e SHALL fazer as barras crescerem em cascata ao renderizar. A visão geral SHALL exibir também um anel com a porcentagem do patrimônio aplicada em ações, contando até o valor enquanto o anel se preenche.

#### Scenario: Carteira com posições
- **WHEN** o usuário tem posições em AAPL, ITUB4, PETR4 e VALE3
- **THEN** as barras aparecem nessa ordem de valor, a de AAPL em destaque, e o anel mostra a fatia investida do patrimônio

#### Scenario: Carteira vazia
- **WHEN** o usuário não tem posições
- **THEN** o card de barras convida a comprar uma ação e o anel mostra 0%
