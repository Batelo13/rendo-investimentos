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
O sistema SHALL manter inalterados todos os endpoints, regras de negócio, cálculos e dados exibidos no dashboard — a mudança é exclusivamente de apresentação visual. Nenhum widget ou indicador SHALL exibir dados de mercado externos (ex.: índices como IBOV) que não sejam fornecidos pelo backend.

#### Scenario: Fluxos existentes continuam funcionando
- **WHEN** o usuário cadastra corretora/ação, compra/vende uma ação, navega entre as abas ou alterna o tema claro/escuro
- **THEN** o comportamento e os valores calculados são idênticos aos anteriores ao redesign, apenas com nova apresentação visual

#### Scenario: Dado de mercado externo não disponível no backend
- **WHEN** um elemento visual do dashboard exigiria um dado de mercado que o backend não expõe (ex.: cotação de um índice como o IBOV)
- **THEN** esse elemento não é exibido, em vez de mostrar um valor inventado ou fixo

### Requirement: Patrimônio total tile on Visão Geral
The Visão Geral stats grid SHALL show a featured "Patrimônio total" tile computed from data already loaded on the page (saldo disponível + valor atual das posições, converted to BRL using the same exchange-rate logic already used for "Resultado não realizado"), with no fabricated or externally-sourced figures (e.g. no benchmark index data).

#### Scenario: Viewing Visão Geral with an open position
- **WHEN** the user has saldo disponível and at least one open position with a known valorAtual
- **THEN** the "Patrimônio total" tile shows saldo disponível plus the BRL-converted value of open positions

### Requirement: Stat tiles show an icon badge
Each tile in the Visão Geral stats grid SHALL display a small icon badge identifying its metric.

#### Scenario: Viewing the stats grid
- **WHEN** the user views Visão Geral
- **THEN** every stat tile shows a small icon consistent with its label

### Requirement: Chart shows axis labels
The evolução patrimonial chart SHALL show the minimum and maximum plotted values on the Y axis and the first/last plotted dates on the X axis.

#### Scenario: Chart has enough points to render
- **WHEN** the chart renders a line (2+ points in the selected period)
- **THEN** the Y-axis shows the min/max values and the X-axis shows the first/last dates of the plotted range

### Requirement: Active period pill uses an outlined style
The currently selected chart period pill SHALL be visually distinguished with an outlined (bordered) style rather than a solid filled background.

#### Scenario: A period is selected
- **WHEN** a period button is active
- **THEN** it shows a visible border in the brand color instead of a filled background
