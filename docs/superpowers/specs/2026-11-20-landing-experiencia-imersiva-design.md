# Página inicial do Rendo: experiência imersiva e interativa

Data: 2026-11-20. Status: aguardando aprovação.

## Intenção

Quem chega à página inicial precisa entender em poucos segundos que o Rendo é um simulador de carteira sério, e ter vontade de continuar. Hoje a página tem uma cena controlada pelo scroll, com boneco, barras e cidade, seguida de seções de texto estático. A cena parece amadora (boneco de retângulos, barras sem dado, fundo vazio), e a única interação é o scroll.

O objetivo é uma jornada em que o visitante **vê e experimenta** o produto, em vez de ler sobre ele. Ela precisa transmitir impacto, confiança, inovação, controle, exclusividade, curiosidade e clareza. O fator "uau" não pode custar credibilidade: é um produto financeiro.

## Metas, não-metas e anti-metas

**Metas**
- Abertura marcante, que mostra o produto ganhando forma conforme o scroll e reage ao mouse com profundidade.
- O visitante monta uma carteira demonstrativa antes do cadastro e vê como ela se comportaria em cenários diferentes.
- As funcionalidades reais (corretoras com CNPJ e CVM, ordens com confirmação, preço médio, câmbio, histórico, evolução) são **demonstradas** por interfaces vivas, não descritas.
- Cada animação tem propósito: dirigir a atenção, explicar uma funcionalidade ou dar retorno a uma ação.
- Página fluida (≈60 fps), responsiva, acessível, com `prefers-reduced-motion` respeitado e SEO básico correto.

**Não-metas**
- Cotações reais na landing (ela é pública e não chama a API).
- Vídeo (decisão do usuário: fica a cena ilustrada).
- 3D/WebGL, bibliotecas de animação e scroll suave sequestrado (Lenis).
- Mudanças no dashboard, no login ou no backend.

**Anti-metas** (falham mesmo que "funcionem")
- Neon, glow e gradiente em excesso; grade de cards repetidos; aparência de template.
- Número fictício que pareça rentabilidade real ou promessa de retorno.
- Engasgo no celular, layout que pula (CLS) ou animação que atrapalha a leitura.
- Sequestro do scroll nativo.

## Restrições

- **Stack:** Thymeleaf + JS puro + CSS, sem build de front. A CSP é `script-src 'self'` (só arquivos do próprio site, nada inline, sem `on*=`); o `LiquidGlassMarkupTest` garante isso.
- **Identidade existente:** `tokens.css`, `glass.css`, Inter (texto) e Baloo 2 (logo), verde `#5DE2BA`/`#5FE1B0` e dourado `#E9C27A` nas etiquetas. Partimos disso, não de uma identidade nova.
- **Motor atual:** `landing.js` calcula o progresso `p` (0..1) da seção sticky com interpolação em `requestAnimationFrame`, e `motion.js` revela `[data-motion]` com IntersectionObserver e Web Animations API. Os dois continuam sendo a base.
- **Dados:** tudo que aparece em número é demonstrativo e rotulado assim ("Dados demonstrativos", "Série ilustrativa: não é histórico real nem previsão").

## Abordagens consideradas

| Abordagem | Troca | Decisão |
|---|---|---|
| **A. Sem dependência nova:** SVG + JS puro + Web Animations API + IntersectionObserver | Mais código à mão na coreografia; peso extra ≈ 0, CSP intacta, controle total do desempenho | **Escolhida pelo usuário** |
| B. GSAP + ScrollTrigger copiados para `js/vendor` | Timelines mais rápidas de escrever; +~70 KB e reescrita do motor, sem ganho visível ao visitante | Descartada |
| C. Three.js (abertura 3D) | Maior impacto inicial; +150 KB, GPU pesada no celular, risco de "blob 3D genérico" | Descartada |

## A jornada (ordem da página)

A narrativa segue os 6 pontos do briefing: o que é, que problema resolve, como ajuda, como funciona, diferenciais, por que usar.

### 0. Cabeçalho
Mantém a estrutura atual: transparente no topo e translúcido depois. Os links passam a apontar para as seções novas: A Rendo, Experimente, Como funciona e Segurança. Ganha microinterações: sublinhado que acompanha o link ativo da seção visível e botão "Criar conta" com brilho que segue o cursor.

### 1. Abertura: "a carteira ganhando forma" (o que é o Rendo)
Substitui o boneco, as barras e a cidade. É a mesma seção sticky de 450vh (320vh no celular), com os mesmos 4 momentos de texto e a copy atual revisada.

O centro da cena é um **cartão de carteira** em vidro (mesma linguagem do dashboard real), em camadas de profundidade sobre um horizonte de grade discreto:

```
 ┌───────────────────────────────────────────────────────────────┐
 │ 01 · COMEÇO                     ·  ·    ╭───────────────────╮ │
 │ Seu futuro é construído,              ╱ │ Patrimônio (demo) │ │
 │ um degrau de cada vez.              ╱   │ R$ 100.000,00     │ │
 │ texto curto…                            │ ───────────────── │ │
 │ [Começar agora] [Conhecer]              ╰───────────────────╯ │
 │ ▔▔▔▔ progresso      · · · · grade do horizonte · · · · · ·   │
 └───────────────────────────────────────────────────────────────┘
```

| Momento | Progresso | O que a cena faz (sempre ligado ao scroll, nos dois sentidos) |
|---|---|---|
| 01 Começo | 0–0,2 | Cartão sozinho com R$ 100.000,00 de saldo e uma linha plana. |
| 02 Planejamento | 0,2–0,45 | Chips de ações chegam de fora e se encaixam num anel de alocação; o saldo se divide entre elas. |
| 03 Evolução | 0,45–0,75 | A linha se desenha num gráfico de evolução (série ilustrativa); marcadores de compra e venda acendem; o contador acompanha. |
| 04 Futuro | 0,75–1 | A câmera recua e o cartão se junta a cartões de posições e operações, formando um mini-dashboard; luz de amanhecer sutil, sem neon; CTA "Construir meu futuro". |

- **Mouse:** inclinação do cartão de até ±6°, parallax das camadas e um reflexo de luz que segue o cursor. Tudo via variáveis CSS (`--mx`, `--my`) atualizadas em rAF e animando só `transform` e `opacity`.
- **Toque:** sem giroscópio, que no iOS pede permissão e assusta. Escolha minha.
- **Movimento reduzido:** cena parada no estado final, momentos em texto corrido (como hoje) e contadores já no valor final.

### 1b. A Rendo em números (seção "visão geral" atual)
A seção atual ("Um simulador de carteira para aprender investindo, sem risco") continua logo depois da abertura. Os fatos (R$ 100.000, Brasil e EUA, zero dinheiro real) passam a usar `data-contador`.

### 2. O problema
Uma seção curta, "Por que começar a investir trava tanta gente", com três problemas reais que o simulador resolve. Cada um aparece como um estado "antes" que se resolve no estado "Rendo" quando entra na tela (e no hover):
1. **Medo de errar com dinheiro real** → saldo fictício, R$ 0 em risco.
2. **Informação espalhada** (corretoras, preço médio, câmbio) → uma linha organizada de posição.
3. **Não saber se está indo bem** → resultado realizado e não realizado lado a lado.

### 3. Experimente: simulador demonstrativo (o centro da página)

```
 ┌ Monte uma carteira de demonstração ──────────── Dados demonstrativos ┐
 │ [PETR4✓] [VALE3✓] [ITUB4] [WEGE3✓] [AAPL] [MSFT]   (escolha até 4)     │
 │ PETR4 ━━━━━━●──── R$ 40.000      ╭─╮ anel de alocação                 │
 │ VALE3 ━━━●─────── R$ 25.000      │ │ caixa: R$ 15.000                  │
 │ WEGE3 ━━●──────── R$ 20.000      ╰─╯                                  │
 │ Cenário: ( Alta ) ( Lateral ) ( Queda )                               │
 │ ╱╲╱‾‾╲╱ gráfico 12 meses · Série ilustrativa: não é histórico real   │
 │ Patrimônio no cenário: R$ 1xx.xxx  ← contador animado                 │
 │ [Criar conta e montar a carteira de verdade]                          │
 └───────────────────────────────────────────────────────────────────────┘
```

- **Ações:** 6 tickers do catálogo real (4 BR, 2 EUA), com monograma no lugar do logo (sem requisição externa). Os preços e as séries são sintéticos e determinísticos (mesma entrada, mesmo gráfico).
- **Alocação:** sliders nativos (`input type=range`, acessíveis); a soma nunca passa de R$ 100.000 e o que sobra aparece como "caixa". Ações dos EUA mostram o valor convertido com uma cotação demo fixa, rotulada.
- **Cenários:** em vez de um "retorno" único, o visitante alterna entre Alta, Lateral e Queda. Isso ensina que o resultado varia e evita parecer promessa. O gráfico transiciona entre cenários, e um resumo em `aria-live` descreve a mudança em texto.
- **Movimento reduzido:** tudo funciona, só que sem transição.
- **Sem estado:** a carteira demo não é salva nem levada para o cadastro, e nada vai ao servidor. Escolha minha.

### 4. Como funciona: tour do produto
Os 4 passos atuais (conta, corretoras, ordens, acompanhamento) viram um **tour fixo**. À esquerda fica a lista de passos; à direita, uma **réplica em HTML/CSS** da tela real de cada passo (não é screenshot), que muda com o scroll ou com um clique no passo:
1. formulário de cadastro e código de 6 dígitos;
2. CNPJ digitado e selo "conferido na CVM";
3. tela de confirmação de ordem;
4. dashboard com posições e evolução.

No celular, vira um stepper sem sticky. As réplicas usam os tokens e o `glass.css` já carregados na landing; o `dashboard.css` não é carregado na página inicial.

### 5. Diferenciais: recursos que se mostram
Três ou quatro blocos **de tamanhos diferentes** (não uma grade igual), cada um com uma microdemonstração:
- **Câmbio:** alterna R$/US$ e o valor convertido conta até o novo número.
- **Histórico de cotação:** o cursor percorre o gráfico e mostra preço e data (ilustrativos).
- **Preço médio:** duas compras se somam num preço médio animado.
- **Confirmação de ordem:** a tela de revisão antes de executar.

Os blocos têm luz que segue o cursor e inclinação leve. Sem neon.

### 6. Por que confiar: segurança
Lista que aparece progressivamente com proteções **que existem de fato**: verificação por e-mail, limite de tentativas de login, CPF protegido, dados isolados por conta, cabeçalhos de segurança/CSP e o gate de segurança semanal. Fica explícito que nenhum dinheiro real circula.

### 7. Chamada final e rodapé
CTA "Comece com R$ 100.000 fictícios", com contador e botão com microinteração magnética sutil. O rodapé fica como está.

## Sistema de movimento (reutilizável)

O `motion.js` ganha comportamentos por atributo, usados em toda a página:
- `data-motion`: revela ao entrar na tela (já existe).
- `data-contador="100000"` (+ `data-formato="brl"`): conta até o valor ao entrar na tela.
- `data-luz`: reflexo que segue o cursor (variáveis CSS).
- `data-inclina`: inclinação leve com o mouse, até ±4°.

**Arquivos (todos externos, CSP ok):**
- `landing.js`: motor de progresso + cena da abertura (substitui o desenho atual).
- `landing-simulador.js`: simulador.
- `landing-tour.js`: tour.
- `landing.css`: reorganizado por seção.

**Regras de desempenho**
- Animar só `transform` e `opacity`.
- `pointermove` agrupado em rAF.
- O que está fora da tela é pausado via IntersectionObserver.
- Geometria SVG recalculada só na abertura e no simulador.
- Dimensões reservadas para não haver CLS.

**Código morto removido:** o ramo de vídeo do `landing.js` (`data-video`, `<video>`, poster), já que a decisão é não usar vídeo. Escolha minha; o arquivo `rendo-jornada-poster.webp` sai junto.

## SEO e acessibilidade

- **SEO:** um único `h1` (o do momento 01), seções semânticas, meta description atual e Open Graph (`og:title`, `og:description`, `og:type`).
- **Teclado:** todo o conteúdo dos momentos e do tour é acessível por teclado (o comportamento atual de focar e rolar é mantido).
- **Leitores de tela:** gráficos com resumo textual; mídia decorativa com `aria-hidden`.
- **Contraste:** AA no texto.

## Segurança

- Nenhum endpoint novo e nenhuma requisição externa nova.
- CSP inalterada: nada inline, e o `LiquidGlassMarkupTest` continua valendo.
- `scripts/dast_zap.sh` precisa continuar aprovado.

## Verificação

- `mvnw test` verde, com testes de markup novos para: rótulos de "dados demonstrativos" presentes, nenhum script ou handler inline e um único `h1`.
- Roteiro Playwright (Chrome) da landing:
  - console sem erros e sem violação de CSP;
  - scroll da abertura para baixo e para cima;
  - simulador: soma limitada a R$ 100.000, troca de cenário e texto `aria-live` atualizado;
  - tour: clique e scroll;
  - `prefers-reduced-motion`;
  - screenshots em 1280, 768 e 390 px.
- Desempenho: trace do Chrome no scroll da abertura sem tarefas longas (>50 ms) recorrentes; CLS < 0,1.
- Gate de segurança e DAST aprovados antes de cada PR.

## Entrega

Três partes, cada uma com sua change OpenSpec, commits pequenos e PR próprio:
1. **Sistema de movimento + abertura nova** (inclui remover o vídeo).
2. **Simulador demonstrativo.**
3. **Problema, tour, diferenciais, segurança e CTA.**

## Decisões minhas (para revisar)

1. **Personagem:** some. A metáfora "um degrau de cada vez" passa para o gráfico em degraus que se forma na abertura.
2. **Tickers:** reais do catálogo, com dados sintéticos e rótulo explícito. Tickers inventados pareceriam menos sérios.
3. **Cenários:** Alta, Lateral e Queda, em vez de um retorno único, para não soar como promessa.
4. **Celular:** sem giroscópio.
5. **Vídeo:** código e poster removidos.
6. **Copy:** escrita por mim, no tom atual (direta, honesta, sem promessa de retorno).

## Fica a cargo de quem implementa

Curvas de easing exatas, os números das séries sintéticas, espaçamentos finos e a escala tipográfica dentro dos tokens existentes.
