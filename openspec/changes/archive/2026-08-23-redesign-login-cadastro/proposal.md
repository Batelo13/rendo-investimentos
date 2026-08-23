## Why

O redesign visual do dashboard (change `redesign-dashboard-visual`) deixou a tela de login/cadastro com uma identidade visual desalinhada — paleta antiga, painéis sem hierarquia, sem elementos que comuniquem "produto financeiro real". Paralelamente, a rodada 1 do redesign do dashboard deixou lacunas apontadas pelo usuário (bug de overflow no filtro de "Ações", falta de destaque para o patrimônio total, tiles sem ícone, gráfico sem rótulos de eixo) que precisavam de uma segunda passada.

## What Changes

- Redesign completo da tela de login/cadastro: paleta própria fixa (painel escuro `#24243A`/`#30304A`, mint `#55DDB1`, tons de texto/fundo claros no lado do formulário), independente do tema claro/escuro do dashboard.
- Painel deslizante existente **preservado** (mesma classe `.active` disparando a troca) — só a técnica de posicionamento mudou de `transform: translateX(±100%)` para `left` em porcentagem, permitindo a proporção 45/55 pedida sem risco matemático à animação.
- Cabeçalho no topo de cada lado do painel escuro ("Já tem uma conta? Entrar →" / "Ainda não tem uma conta? Criar conta →") reaproveitando os mesmos botões/IDs que já disparavam a animação.
- Lista de destaques ampliada de 3 para 4 itens por lado, com selo circular verde.
- Widgets financeiros decorativos e **estáticos** (Patrimônio total com sparkline, card de cotação PETR4, card de segurança) — sem nenhuma chamada nova a API/serviço/banco.
- Toggle de mostrar/ocultar senha nos campos de senha (login e cadastro).
- Responsivo mobile: abaixo de 700px o painel escuro vira uma faixa compacta no topo (sem os widgets/checklist) e a troca de estado deixa de ser um slide horizontal (não há espaço pra dois painéis de 45%/55%) e passa a ser uma troca direta de exibição, mantendo a mesma lógica `.active`.
- Nenhum botão de login social adicionado (confirmado: não existe OAuth2 configurado no backend).
- Correção do bug de overflow do input `.filter-input` em telas estreitas (`min-width: 0` + `flex-wrap` no `.panel-head`).
- Dashboard / Visão geral: tile "Patrimônio total" (saldo disponível + valor das posições, dado real derivado do estado já carregado), ícone em todos os stat-cards, rótulos de eixo X/Y no gráfico de evolução patrimonial, pill de período ativo com estilo outlined em vez de preenchido.
- Explicitamente **não implementado** (documentado, não esquecido): widget de índice IBOV (dado de mercado externo que o backend não fornece), sparkline dedicado ao "Resultado não realizado" (não há série histórica decomposta desse valor) e menu de três pontos por linha em "Minhas posições" (sem ação de backend para popular).

## Capabilities

### New Capabilities
(nenhuma)

### Modified Capabilities
- `login-mais-rico`: destaques passam de 3 para 4 itens, ganham paleta própria fixa, cabeçalho de troca de estado no topo do painel, widgets financeiros decorativos estáticos, toggle de mostrar/ocultar senha, e comportamento responsivo dedicado abaixo de 700px.
- `redesign-dashboard-visual`: correção do overflow do filtro de "Ações", tile de patrimônio total, ícones nos stat-cards, rótulos de eixo no gráfico e pill de período outlined.

## Impact

- `src/main/resources/templates/login.html`, `static/css/login.css`, `static/js/login.js`
- `src/main/resources/static/css/dashboard.css`, `templates/dashboard.html`, `static/js/dashboard.js`
- Nenhum arquivo `.java`, endpoint, DTO, entidade ou configuração de segurança foi alterado.
