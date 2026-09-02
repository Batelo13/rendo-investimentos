## Purpose

Mostrar, na Visão Geral do dashboard, um preço médio único por ação que combina todas as corretoras em que o usuário a possui — em vez de repetir o preço médio de cada posição (ação+corretora) em linhas separadas.

## Requirements

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

### Requirement: Indicação visual de múltiplas corretoras
O sistema SHALL exibir a contagem de corretoras (em vez do nome de uma corretora específica) quando a linha consolidada de uma ação combinar posições de mais de uma corretora.

#### Scenario: Selo de contagem
- **WHEN** uma ação consolidada combina posições de 2 ou mais corretoras
- **THEN** o selo e o texto de corretora mostram a contagem (ex.: "×2", "2 corretoras") em vez do nome de uma corretora

### Requirement: Tabela detalhada "Minhas posições" permanece por corretora
O sistema SHALL manter a tabela completa "Minhas posições" inalterada: uma linha por combinação de ação e corretora, cada uma com seu próprio preço médio acumulado.

#### Scenario: Duas posições da mesma ação na tabela completa
- **WHEN** o usuário possui a mesma ação em duas corretoras
- **THEN** a tabela completa "Minhas posições" continua exibindo duas linhas separadas, uma por corretora, cada uma com Vender independente

### Requirement: Ação "Vender" removida da mini-lista consolidada
O sistema SHALL não exibir uma ação rápida de venda na mini-lista consolidada da Visão Geral, já que uma linha agregada não corresponde a uma única posição vendível.

#### Scenario: Usuário quer vender a partir da Visão Geral
- **WHEN** o usuário está na Visão Geral e quer vender uma posição
- **THEN** ele precisa navegar para "Minhas posições" para escolher a corretora específica e vender
