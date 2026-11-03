## Why

O Rendo já possui uma identidade fintech consistente, mas suas telas usam
tratamentos de superfície e movimento diferentes entre dashboard, autenticação,
landing e loading. Uma linguagem liquid glass discreta e profissional cria
continuidade visual, melhora hierarquia e dá acabamento equivalente aos temas
claro e escuro sem mudar dados ou fluxos de negócio.

## What Changes

- Introduz um sistema compartilhado de superfícies de vidro, profundidade,
  reflexos e fundos atmosféricos baseado nos tokens atuais do Rendo.
- Reorganiza moderadamente dashboard, autenticação e landing para trabalhar com
  três planos visuais: fundo, estrutura e conteúdo.
- Aplica acabamento equivalente nos temas claro e escuro, inclusive na tela de
  autenticação, preservando menta, roxo-ardósia, logo e tipografia da marca.
- Adiciona entradas blur-para-nítido, revelação sequencial limitada,
  microinterações de hover/press e transições de modal/navegação.
- Mantém todo o conteúdo utilizável sem JavaScript de movimento e fornece
  superfícies opacas equivalentes quando `backdrop-filter` não estiver
  disponível.
- Respeita `prefers-reduced-motion`, foco por teclado, contraste e dispositivos
  sem hover.
- Preserva IDs, endpoints, regras de negócio, cálculos, dados e fluxos atuais.

## Capabilities

### New Capabilities

- `liquid-glass-interface`: sistema visual compartilhado de superfícies,
  movimento progressivo, responsividade, acessibilidade e fallback aplicado a
  todas as telas.

### Modified Capabilities

- `redesign-dashboard-visual`: o dashboard passa a usar composição em camadas,
  superfícies glass e microinterações sem alterar seus dados ou ações.
- `login-mais-rico`: autenticação deixa de ter uma única paleta fixa e recebe
  variantes glass equivalentes para os temas claro e escuro, preservando o
  fluxo deslizante e o painel de marca.
- `loading-screen`: a saída do loading passa a coordenar blur-para-nítido com o
  conteúdo, mantendo exatamente as condições atuais de exibição.
- `tema-claro-escuro`: a preferência persistida passa a produzir acabamento
  glass completo também na autenticação e landing, sem flash do tema incorreto.

## Impact

- CSS: `tokens.css`, novo `glass.css`, `dashboard.css`, `login.css` e
  `loading.css`.
- JavaScript: novo `motion.js`, além de integração pontual com `loading.js`,
  `theme.js`, `login.js` e `dashboard.js` quando necessária para estados
  visuais já existentes.
- Templates: `dashboard.html`, `login.html`, `index.html` e fragmentos de
  loading/logo apenas para carregar os recursos compartilhados e adicionar
  wrappers/classes de apresentação.
- Testes: ampliação dos testes de marcação e validação visual/funcional em tema
  claro, tema escuro, viewport móvel, redução de movimento e fallback sem blur.
- Sem alterações em API, banco de dados, DTOs, entidades ou dependências de
  runtime.
