## Why

O termo "Preço médio acumulado" foi introduzido na tabela detalhada "Minhas posições" (ver histórico do mesmo dia) para diferenciá-lo do preço médio consolidado por ação recém-adicionado na Visão Geral. O usuário decidiu que essa distinção de nomenclatura não é necessária: ambas as telas devem usar apenas "Preço médio".

## What Changes

- Remove a palavra "acumulado" de todos os textos visíveis: cabeçalho da tabela "Minhas posições", dica de compra, texto informativo do modal de venda e dica de venda.
- Nenhuma mudança de cálculo — apenas rótulo. O valor por corretora na tabela detalhada continua sendo a média ponderada de `PosicaoCalculator`; o valor na Visão Geral continua sendo o agregado por ação.

## Capabilities

### Modified Capabilities
- `preco-medio-consolidado-por-acao`: remove a menção ao termo "acumulado" como forma de diferenciar as duas telas — agora ambas usam o mesmo rótulo "Preço médio".

## Impact

- `src/main/resources/templates/dashboard.html`: cabeçalho da tabela "Minhas posições" volta a "Preço médio".
- `src/main/resources/static/js/dashboard.js`: 3 textos (dica de compra, info do modal de venda, dica de venda) voltam a "preço médio" sem "acumulado".
