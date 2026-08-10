# Identidade Visual — Design

Data: 2026-08-10
Branch prevista: `12-identidade-visual`

## Contexto

O backend do Rendo (Java/Spring Boot) está praticamente completo — usuários, autenticação, corretoras, ações, carteira, operações, saldo virtual. O frontend ainda não existe (só o Thymeleaf padrão do Spring Initializr). Antes de construir qualquer tela, a estratégia definida foi: fixar a identidade visual primeiro, telas depois, gradualmente.

Este documento formaliza as decisões tomadas na sessão de brainstorming visual (companheiro no navegador) de 2026-08-10.

## Escopo

- Nome, paleta de cores, tipografia, símbolo/logo, estilo base de componentes (cards, botões, inputs, tabelas), direção da animação de loading.
- **Não** inclui: construção das telas em si, CSS/Thymeleaf de verdade, biblioteca de gráficos (fica pra quando o dashboard for construído), animações detalhadas além da direção geral.

## Nome

**Rendo** — já era o nome informal do projeto (`pom.xml` `<name>Rendo</name>`, `spring.application.name=rendo`). Remete a "renda"/rendimento, encaixa no domínio de investimentos. Decisão: manter, sem explorar alternativas.

## Paleta de cores

Direção escolhida: **Roxo Ardósia + Menta** — foge do clichê "verde de dinheiro" (evita parecer Robinhood/genérico), mais próximo de SaaS moderno/fintech do que de banco tradicional. Fundo escuro (dark-first).

| Papel | Cor | Hex |
|---|---|---|
| Primária (marca, botões principais) | Roxo ardósia | `#4C4B63` |
| Secundária / Sucesso / Crescimento | Menta | `#5FE1B0` |
| Fundo (base da página) | Quase-preto arroxeado | `#1B1A24` |
| Superfície (cards, painéis) | Um tom acima do fundo | `#24232F` |
| Texto principal | Quase-branco levemente lilás | `#EDEBF5` |
| Texto secundário | Cinza-lilás apagado | `#B7B5C4` |
| Alerta | Âmbar suave | `#F2B84B` |
| Erro | Coral | `#F07167` |
| Borda sutil (cards/inputs) | Roxo ardósia bem apagado | `#34333f` / `#3A3948` |

Sucesso e "cor secundária" são a mesma cor (menta) — decisão deliberada: no domínio de investimentos, "crescimento" e "sucesso" são conceitualmente a mesma coisa, não precisa de uma terceira cor só pra isso.

## Tipografia

**Inter** (Google Fonts) para título e corpo — pesos 400 (texto), 600 (ênfase), 700 (títulos/wordmark). Escolhida em vez de combinações com mais personalidade (Space Grotesk, Sora+IBM Plex) porque o objetivo é legibilidade e um visual "SaaS profissional" limpo, sem chamar atenção pra tipografia em si.

## Símbolo / Logo

**Carteira clássica (billfold)**, não um monograma "R" nem uma moeda — decisão final depois de testar as duas ideias, porque o app é literalmente uma carteira de ações, e o símbolo deve dizer isso diretamente.

Especificação do ícone (SVG, 64×64 viewBox, escala livremente):

```html
<svg viewBox="0 0 64 64">
  <rect x="8" y="18" width="48" height="34" rx="7" fill="#24232F" stroke="#4C4B63" stroke-width="3"/>
  <path d="M8 30 H56" stroke="#4C4B63" stroke-width="2" opacity="0.5"/>
  <circle cx="44" cy="35" r="4" fill="#5FE1B0"/>
</svg>
```

Composição: corpo da carteira (retângulo arredondado, contorno roxo ardósia, preenchimento cor de superfície), uma linha horizontal sutil sugerindo a dobra, e um "botão"/fecho circular em menta. Funciona em tamanho pequeno (favicon ~16-32px) — testado mentalmente, sem detalhe fino demais.

Wordmark ao lado do símbolo: "Rendo" em Inter 700.

## Estilo de componentes

**Com borda sutil** (não "flutuante sem borda") — cards, inputs e tabelas usam contorno de 1px em `#34333f`/`#3A3948` por cima da cor de superfície `#24232F`. Cada bloco fica claramente definido contra o fundo, sem depender só de diferença de tom.

Botões:
- Primário: fundo `#4C4B63`, texto `#EDEBF5`, sem borda.
- Secundário: fundo transparente, texto `#EDEBF5`, borda 1px `#4C4B63`.

Raio de borda: 8px em botões/inputs, 10px em cards. Sem sombras pesadas, sem gradiente — consistente com o pedido original de design minimalista (seção 18 do prompt original do usuário).

## Animação de loading

Decisão importante: o pedido original descrevia uma **moeda estilizada girando**. Como o símbolo final virou uma **carteira** (não moeda), a animação de loading foi realinhada pra usar o mesmo símbolo da marca, em vez de um motivo desconectado. Direção geral (detalhamento técnico — CSS vs. SVG vs. Canvas — fica pra quando a tela de loading for construída, como já era o plano original): algo sutil envolvendo a carteira (ex: o botão mint pulsando, ou a carteira "abrindo" levemente), não uma rotação 3D como a moeda original sugeria — o formato de carteira não tem a simetria circular que fazia sentido girar.

## Fora de escopo (adiar pra quando as telas forem construídas)

- Biblioteca de gráficos para o dashboard.
- CSS/Thymeleaf reais (só a especificação de design tokens está aqui).
- Detalhamento técnico da animação de loading (CSS/SVG/Canvas).
- Ícones adicionais além do símbolo da marca (ex: ícones de navegação, categorias).
