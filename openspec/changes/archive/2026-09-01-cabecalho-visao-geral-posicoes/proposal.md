## Why

A mini-lista "Minhas posições" da Visão Geral nunca teve um cabeçalho de colunas (era só uma sequência de linhas sem rótulo). Depois de consolidar o preço médio por ação nessa mesma lista (`preco-medio-consolidado-por-acao`), o professor pediu explicitamente um cabeçalho ali — e que o rótulo seja apenas "Preço médio" (não "Preço médio acumulado", que é o termo usado na tabela detalhada "Minhas posições", pois ali o valor é por corretora, não consolidado).

## What Changes

- Adiciona uma linha de cabeçalho fixa acima da mini-lista da Visão Geral: Ativo, Corretora, Qtd, Preço médio, Preço atual, Resultado, Valor.
- O cabeçalho usa o mesmo grid de colunas da mini-lista (`.mini-row`) e os mesmos breakpoints responsivos (esconde as mesmas colunas nas mesmas larguras).
- Rótulo da coluna é "Preço médio" (sem "acumulado"), para diferenciar do valor por corretora da tabela completa.

## Capabilities

### Modified Capabilities
- `preco-medio-consolidado-por-acao`: adiciona o requisito de cabeçalho de colunas à mini-lista da Visão Geral.

## Impact

- `src/main/resources/templates/dashboard.html`: nova linha `.mini-row.mini-row-header` estática antes de `#dashPosicoes`.
- `src/main/resources/static/css/dashboard.css`: estilo do cabeçalho (`.mini-row-header`) e ocultação das mesmas colunas nos 2 breakpoints responsivos já existentes.
- Nenhuma mudança de backend.
