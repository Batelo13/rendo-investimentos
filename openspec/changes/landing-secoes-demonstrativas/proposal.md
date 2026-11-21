## Why

Depois do simulador, a página inicial ainda descreve o produto com texto estático. O briefing pede que as funcionalidades sejam **demonstradas** e que a jornada mostre problema, funcionamento, diferenciais e motivos para confiar. Esta é a parte 3 do design `docs/superpowers/specs/2026-11-20-landing-experiencia-imersiva-design.md`.

## What Changes

- **"O problema"**: três dificuldades que se resolvem na tela (antes → depois), com o atributo `data-resolve` no `motion.js`.
- **"Como funciona"** vira um tour com réplicas em HTML/CSS das telas reais (cadastro, corretora, ordem, dashboard) e um índice que marca o passo ativo.
- **"Diferenciais"** (`#recursos`): três blocos com microdemonstrações (câmbio, histórico de cotação, preço médio) e uma linha "Também na Rendo:".
- **Segurança** passa a listar só proteções que existem de fato e destaca que nenhum dinheiro real circula.
- **Chamada final** com contador "R$ 100.000" e botão magnético (`data-ima` no `motion.js`).
- **Removidos**: a seção "Acompanhamento" (o tour já mostra o dashboard) e o link "Recursos" do cabeçalho.

## Impact

- **Endpoints, banco e dependências:** nenhum endpoint novo ou alterado, sem banco, sem dependência.
- **Arquivos:** `index.html`, `landing.css`, `glass.css` e `motion.js`, mais os novos `landing-tour.js` e `landing-diferenciais.js`.
