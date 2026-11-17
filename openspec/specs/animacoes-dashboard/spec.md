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

