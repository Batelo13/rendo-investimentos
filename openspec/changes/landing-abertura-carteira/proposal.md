## Why

A abertura da página inicial (boneco, barras e cidade) parece amadora, e a única interação é o scroll. O usuário decidiu não usar vídeo e quer uma experiência imersiva, profissional e interativa (design em `docs/superpowers/specs/2026-11-20-landing-experiencia-imersiva-design.md`, parte 1).

## What Changes

- **Abertura nova:** um cartão de carteira demonstrativa em vidro que ganha forma com o scroll. O saldo de R$ 100.000 se divide em 4 ações num anel, a linha de evolução ilustrativa se desenha e, no fim, cartões de posições e operações formam um mini-dashboard. O cartão inclina e reflete luz com o ponteiro; tudo fica rotulado "Dados demonstrativos".
- **Sistema de movimento reutilizável** no `motion.js`, por atributo: `data-contador`, `data-luz` e `data-inclina`.
- **Cabeçalho:** marca o link da seção visível; "Criar conta" com luz. Tags Open Graph.
- **BREAKING:** sai o suporte a vídeo (`<video>`, poster, código de sincronização) e a rota pública **`GET /videos/**`**, que passa a exigir sessão como qualquer outra.

## Impact

- **Endpoints públicos:** `GET /videos/**` removido. Nenhum endpoint novo.
- **Banco e dependências:** sem mudança.
- **Arquivos:** `index.html`, `landing.js`, `landing.css`, `motion.js`, `glass.css` e `SecurityConfig`; sai `images/rendo-jornada-poster.webp`.
