## Why

O briefing pede que o visitante experimente uma parte da proposta do Rendo antes do cadastro, vendo a funcionalidade em vez de ler sobre ela. Esta é a parte 2 do design `docs/superpowers/specs/2026-11-20-landing-experiencia-imersiva-design.md`.

## What Changes

- **Seção nova "Experimente"** na página inicial, logo depois de "A Rendo", com link no cabeçalho.
- **Escolha e distribuição:** o visitante escolhe até 4 de 6 ações demo e distribui os R$ 100.000 com sliders. A soma nunca passa do limite, e o que sobra fica em caixa. As ações dos EUA mostram o valor convertido com uma cotação demonstrativa fixa.
- **Cenários:** Alta, Lateral e Queda mostram anel de alocação, gráfico de 12 meses e patrimônio do cenário (contador). Há um resumo acessível em `aria-live`.
- **Tudo rotulado:** "Dados demonstrativos" e "Série ilustrativa: não é histórico real nem previsão.".

## Impact

- **Endpoints e banco:** nenhum endpoint novo nem alterado; sem mudança no banco. Tudo roda no navegador e nada é enviado ou salvo.
- **Dependências:** nenhuma.
- **Arquivos:** `index.html`, `landing.css` e o novo `landing-simulador.js`.
