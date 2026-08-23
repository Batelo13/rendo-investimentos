## Context

O painel deslizante do login (`login.css`) já usava `.form-container.sign-in/.sign-up { position:absolute; width:50%; transition:all 0.6s; }` + `transform: translateX(±100%)` para trocar de lado, junto de um `.toggle-container` espelhado. Essa técnica só funciona de forma limpa quando os dois lados têm a mesma largura, porque `translateX(100%)` desloca o elemento por 100% da SUA PRÓPRIA largura — em uma divisão 50/50 isso coincidentemente é igual à distância necessária para o elemento ocupar o outro lado. O pedido do usuário era uma divisão 45/55 (painel escuro menor que o formulário), que quebra essa coincidência.

## Goals / Non-Goals

**Goals:**
- Suportar a proporção 45/55 sem reescrever a lógica de disparo da animação (`container.classList.add/remove('active')`).
- Manter a paleta do dashboard (`tokens.css`) intocada — a tela de auth precisa de uma identidade fixa própria.
- Cobrir mobile sem forçar os dois painéis a dividirem 50/50 a força numa tela estreita.

**Non-Goals:**
- Não mexer em nenhuma regra de validação de formulário, endpoint ou autenticação.
- Não adicionar login social sem suporte real de OAuth2 no backend.
- Não buscar dados de mercado externos (ex.: IBOV) para os widgets decorativos.

## Decisions

- **`left` em vez de `transform: translateX`**: como `left` em porcentagem é relativo ao container (não ao próprio elemento), `.sign-in`/`.sign-up` com `width: 55%` movem-se para `left: 45%` (a largura do painel escuro) para ocupar o slot correto, e o `.toggle-container` (width 45%) move para `left: 0`. Isso vale para qualquer proporção, não só 50/50, sem precisar calcular porcentagens "mágicas" relativas à própria largura do elemento. Alternativa considerada: manter `translateX` com valores calculados manualmente (ex.: `translateX(81.82%)`) — descartada por ser mais frágil a mudanças futuras de proporção e mais difícil de revisar.
- **Cores fixas via redeclaração de custom properties em `.login-page`**: em vez de criar uma segunda família de tokens (`--auth-*`) espalhada pelo CSS, o bloco `.login-page { --background: ...; --surface: ...; ... }` redeclara os MESMOS nomes de variável já usados no resto do `login.css`, então nenhuma regra existente (`var(--surface)`, `var(--text-primary)` etc.) precisou mudar. Efeito colateral encontrado durante o teste: `color` no `body` já resolvia `var(--text-primary)` no escopo do `body` (fora de `.login-page`), então um `<h1>` sem `color` próprio herdava o valor computado ali, não o override — corrigido redeclarando `color: var(--text-primary)` explicitamente dentro de `.login-page`.
- **Mobile (<700px): troca de exibição em vez de slide**: abaixo de 700px não há espaço horizontal para dois painéis de 45%/55%. Em vez de forçar a mesma animação numa proporção inviável, o layout muda para empilhado (painel escuro vira faixa compacta no topo, largura 100%) e a troca de estado passa a ser um `display:none↔flex` condicionado pela mesma classe `.active` — sem introduzir nova lógica de estado no JS.
- **Widgets financeiros decorativos como HTML/CSS estático**: nenhum dado novo é buscado; os valores (Patrimônio total, PETR4, etc.) são texto fixo no template, com `aria-hidden="true"` no contêiner por serem puramente ilustrativos.
- **"Patrimônio total" do dashboard usa dado real**: reaproveita a mesma lógica de conversão de câmbio já usada para "Resultado não realizado" (`moedaPorTicker`/`taxaCambioPorTicker`), somando `saldoDisponivel` + valor das posições convertido — sem componente "hoje"/delta, porque não há um saldo-base do dia armazenado para calcular essa variação honestamente.

## Risks / Trade-offs

- [Divisão 45/55 muda a matemática da animação] → Mitigado testando manualmente as duas transições (login→cadastro e cadastro→login) várias vezes no navegador antes de considerar a mudança concluída.
- [Layout mobile sem slide pode parecer "menos refinado" que o desktop] → Aceito deliberadamente: um swap instantâneo é mais robusto do que tentar recriar a mesma animação numa proporção de tela onde ela não cabe, e é um padrão comum em produtos fintech reais.
- [`resize_window` da ferramenta de automação do navegador não reflete `window.innerWidth` real nesta sessão] → Contornado testando os breakpoints via `<iframe>` com largura fixa apontando para a mesma origem (o iframe estabelece seu próprio contexto de viewport), o que permitiu verificação visual real em vez de apenas inspeção estática do CSS.
