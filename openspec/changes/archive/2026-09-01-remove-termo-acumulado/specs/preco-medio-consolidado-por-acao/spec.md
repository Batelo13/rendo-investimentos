## MODIFIED Requirements

### Requirement: Consolidação de posições por ação na Visão Geral
O sistema SHALL agrupar as posições exibidas na mini-lista "Minhas posições" da Visão Geral por `acaoTicker`, somando a quantidade e o valor investido de todas as corretoras e calculando o preço médio consolidado como `somaValorInvestido / somaQuantidade`. A mini-lista SHALL exibir um cabeçalho de colunas (Ativo, Corretora, Qtd, Preço médio, Preço atual, Resultado, Valor) acima das linhas, com a coluna de preço rotulada "Preço médio" — mesmo rótulo usado na tabela detalhada por corretora, por decisão explícita do usuário (o termo "acumulado" foi removido de ambas as telas).

#### Scenario: Mesma ação em duas corretoras
- **WHEN** o usuário possui a mesma ação em duas corretoras diferentes, com preços médios distintos em cada uma
- **THEN** a Visão Geral exibe uma única linha para essa ação, com quantidade somada e preço médio ponderado pelas duas posições

#### Scenario: Ação em uma única corretora
- **WHEN** o usuário possui uma ação em apenas uma corretora
- **THEN** a linha consolidada exibe o mesmo preço médio e quantidade da posição original (a agregação não altera o valor de uma posição isolada)

#### Scenario: Cabeçalho de colunas visível
- **WHEN** o usuário abre a Visão Geral
- **THEN** a mini-lista "Minhas posições" exibe um cabeçalho com as colunas Ativo, Corretora, Qtd, Preço médio, Preço atual, Resultado e Valor, alinhado com as linhas de dados

### Requirement: Tabela detalhada "Minhas posições" permanece por corretora
O sistema SHALL manter a tabela completa "Minhas posições" inalterada: uma linha por combinação de ação e corretora, cada uma com seu próprio preço médio.

#### Scenario: Duas posições da mesma ação na tabela completa
- **WHEN** o usuário possui a mesma ação em duas corretoras
- **THEN** a tabela completa "Minhas posições" continua exibindo duas linhas separadas, uma por corretora, cada uma com Vender independente
