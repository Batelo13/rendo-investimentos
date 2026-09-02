## MODIFIED Requirements

### Requirement: Consolidação de posições por ação na Visão Geral
O sistema SHALL agrupar as posições exibidas na mini-lista "Minhas posições" da Visão Geral por `acaoTicker`, somando a quantidade e o valor investido de todas as corretoras e calculando o preço médio consolidado como `somaValorInvestido / somaQuantidade`. A mini-lista SHALL exibir um cabeçalho de colunas (Ativo, Corretora, Qtd, Preço médio, Preço atual, Resultado, Valor) acima das linhas, com a coluna de preço rotulada apenas "Preço médio" (sem "acumulado", termo reservado à tabela detalhada por corretora).

#### Scenario: Mesma ação em duas corretoras
- **WHEN** o usuário possui a mesma ação em duas corretoras diferentes, com preços médios distintos em cada uma
- **THEN** a Visão Geral exibe uma única linha para essa ação, com quantidade somada e preço médio ponderado pelas duas posições

#### Scenario: Ação em uma única corretora
- **WHEN** o usuário possui uma ação em apenas uma corretora
- **THEN** a linha consolidada exibe o mesmo preço médio e quantidade da posição original (a agregação não altera o valor de uma posição isolada)

#### Scenario: Cabeçalho de colunas visível
- **WHEN** o usuário abre a Visão Geral
- **THEN** a mini-lista "Minhas posições" exibe um cabeçalho com as colunas Ativo, Corretora, Qtd, Preço médio, Preço atual, Resultado e Valor, alinhado com as linhas de dados
