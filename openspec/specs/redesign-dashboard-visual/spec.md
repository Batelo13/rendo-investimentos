## Purpose

Define a linguagem visual "fintech premium" do dashboard (tokens, densidade, hierarquia) e as duas capacidades interativas novas do gráfico de evolução patrimonial, preservando o comportamento e os dados já existentes.

## Requirements

### Requirement: Filtro de período no gráfico de evolução patrimonial
O sistema SHALL permitir filtrar os pontos do gráfico de evolução patrimonial por período (1D, 1M, 3M, 6M, 1A ou Tudo), sem alterar os dados originais retornados pela API.

#### Scenario: Selecionar um período com dados suficientes
- **WHEN** o usuário seleciona um período (ex.: "1M") que contém 2 ou mais pontos dentro da janela de tempo
- **THEN** o gráfico é redesenhado mostrando somente os pontos daquele período

#### Scenario: Selecionar um período sem dados suficientes
- **WHEN** o usuário seleciona um período que contém menos de 2 pontos dentro da janela de tempo
- **THEN** o sistema exibe uma mensagem de estado vazio específica de período, sem quebrar a página

### Requirement: Tooltip on-hover no gráfico
O sistema SHALL exibir, ao passar o mouse sobre o gráfico de evolução patrimonial, um tooltip com a data e o valor do ponto mais próximo do cursor.

#### Scenario: Mouse sobre a área do gráfico
- **WHEN** o usuário move o mouse sobre a área do gráfico com pontos suficientes para desenhar uma linha
- **THEN** o sistema exibe um tooltip próximo ao ponto mais próximo do cursor, com a data e o valor formatados

#### Scenario: Mouse sai da área do gráfico
- **WHEN** o cursor deixa a área do gráfico
- **THEN** o tooltip e o destaque do ponto desaparecem

### Requirement: Preservação de comportamento e dados existentes
O sistema SHALL manter inalterados todos os endpoints, regras de negócio, cálculos e dados exibidos no dashboard — a mudança é exclusivamente de apresentação visual.

#### Scenario: Fluxos existentes continuam funcionando
- **WHEN** o usuário cadastra corretora/ação, compra/vende uma ação, navega entre as abas ou alterna o tema claro/escuro
- **THEN** o comportamento e os valores calculados são idênticos aos anteriores ao redesign, apenas com nova apresentação visual
