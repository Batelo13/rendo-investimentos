## Why

O título "Evolução patrimonial" do dashboard aparecia com cerca de 32px, o dobro dos outros títulos de painel. Ele fica num cabeçalho de gráfico que não recebia o estilo de título de painel e herdava o `h2` padrão do Bootstrap.

## What Changes

- O título do cabeçalho do gráfico passa a usar o mesmo estilo dos demais títulos de painel: 16px, peso 700, sem margem.

## Impact

- **Arquivos:** `static/css/dashboard.css` (um seletor) e `LiquidGlassMarkupTest` (um teste).
- **Endpoints, banco e dependências:** sem mudança.
