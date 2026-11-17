# animacoes-dashboard Specification

## Purpose
TBD - created by archiving change animacoes-e-ajustes-visuais. Update Purpose after archive.
## Requirements
### Requirement: Indicadores animam até o valor
O dashboard SHALL animar os indicadores numéricos do valor mostrado anteriormente até o novo valor, e terminar exatamente no texto formatado do valor real.

#### Scenario: Primeira carga da visão geral
- **WHEN** o usuário abre a visão geral
- **THEN** os indicadores contam a partir de zero e terminam no valor formatado da carteira

#### Scenario: Movimento reduzido
- **WHEN** o sistema operacional pede movimento reduzido
- **THEN** os indicadores mostram o valor final direto, sem contagem

### Requirement: Gráficos se desenham ao renderizar
O dashboard SHALL desenhar progressivamente a linha do gráfico de rendimento e as sparklines quando são renderizados.

#### Scenario: Troca de período do gráfico
- **WHEN** o usuário troca o período do gráfico de rendimento
- **THEN** a nova linha é desenhada da esquerda para a direita

### Requirement: Transição entre seções
O dashboard SHALL fazer os blocos da seção escolhida entrarem em cascata, com fade e desfoque, ao trocar de seção. Os cards de indicador SHALL subir levemente no hover em dispositivos com ponteiro.

#### Scenario: Usuário troca de seção
- **WHEN** o usuário clica em outra seção no menu
- **THEN** os blocos da nova seção entram em sequência

### Requirement: Gráfico de barras por ação e anel de capital investido
A visão geral SHALL exibir uma barra por ação em carteira, com o valor atual em R$ (as maiores primeiro e a maior em destaque), e SHALL fazer as barras crescerem em cascata ao renderizar. A visão geral SHALL exibir também um anel com a porcentagem do patrimônio aplicada em ações, contando até o valor enquanto o anel se preenche.

#### Scenario: Carteira com posições
- **WHEN** o usuário tem posições em AAPL, ITUB4, PETR4 e VALE3
- **THEN** as barras aparecem nessa ordem de valor, a de AAPL em destaque, e o anel mostra a fatia investida do patrimônio

#### Scenario: Carteira vazia
- **WHEN** o usuário não tem posições
- **THEN** o card de barras convida a comprar uma ação e o anel mostra 0%

