# Liquid Glass em Todas as Telas — Design

Data: 2026-11-03

## Contexto

O Rendo já possui uma identidade visual consolidada, dark-first, com menta como
cor de marca, neutros quase pretos, suporte a tema claro e componentes próprios
para autenticação e dashboard. O objetivo desta mudança é evoluir essa
identidade para uma linguagem de *liquid glass* discreta e profissional, sem
substituir a paleta nem transformar o produto em uma demonstração de efeitos.

Três gravações fornecidas pelo usuário foram tratadas somente como referência
visual. Os elementos aproveitados são:

- superfícies translúcidas em camadas;
- transição de desfoque para nitidez durante a entrada;
- cards com bordas suaves, cantos amplos e profundidade controlada;
- elevação curta no hover;
- animações progressivas de conteúdo e gráficos.

Textos, marca, cores e estrutura dos produtos mostrados nos vídeos não fazem
parte da implementação.

## Objetivos

- Aplicar uma linguagem visual coesa de vidro em todas as telas do sistema.
- Dar o mesmo nível de acabamento aos temas claro e escuro.
- Permitir uma reorganização moderada dos layouts para reforçar hierarquia e
  profundidade.
- Adicionar animações de alta qualidade sem prejudicar legibilidade,
  acessibilidade ou desempenho.
- Preservar toda a lógica funcional, os endpoints e a identidade cromática do
  Rendo.

## Escopo

### Incluído

- Dashboard e todos os seus estados: navegação, resumo financeiro, gráficos,
  tabelas, filtros, menus, modais, alertas, carregamento e conteúdo vazio.
- Login, cadastro e verificação de email.
- Landing/index e tela de loading compartilhada.
- Temas claro e escuro, desktop e viewport móvel.
- Fallback visual para navegadores sem `backdrop-filter`.
- Respeito a `prefers-reduced-motion`.

### Fora de escopo

- Mudanças no backend, nas regras de negócio ou nos contratos da API.
- Novas funcionalidades de investimento.
- Troca da paleta, logo ou tipografia da marca.
- Paralaxe de cursor, partículas pesadas, WebGL ou animações decorativas que
  disputem atenção com os dados.
- Cópia fiel dos dashboards vistos nas referências.

## Direção visual

O sistema usará três planos de profundidade:

1. **Fundo atmosférico:** base do tema atual com manchas suaves em menta e
   roxo-ardósia. O movimento será lento, amplo e quase imperceptível.
2. **Estrutura:** sidebar, cabeçalho e contêineres principais usarão vidro mais
   denso para garantir contraste e orientar a navegação.
3. **Conteúdo:** cards, tabelas, filtros, formulários e modais usarão vidro
   intermediário, bordas de luz finas e sombras difusas.

O efeito não dependerá apenas de transparência. Cada superfície combinará cor
semitransparente, `backdrop-filter`, borda com luminância controlada, sombra e
um reflexo interno muito discreto. Isso mantém o resultado legível quando há
pouca variação visual atrás do elemento.

## Sistema de tokens

`tokens.css` continuará sendo a origem das decisões visuais. Serão adicionados
tokens semânticos para, no mínimo:

- fundo atmosférico e cores dos halos;
- vidro sutil, vidro padrão e vidro denso;
- borda de vidro e borda destacada;
- brilho interno e sombra ambiente;
- intensidade de blur;
- duração curta, média e longa;
- curvas de entrada, saída e interação.

Os dois temas terão a mesma hierarquia e qualidade, mas valores próprios. O
tema escuro usará superfícies claras em baixa opacidade sobre os neutros atuais;
o tema claro usará branco translúcido, bordas um pouco mais definidas e sombras
mais suaves. Menta, sucesso, alerta e erro permanecem cores semânticas fixas.

## Componentes compartilhados

Um novo `glass.css` reunirá primitives reutilizáveis de apresentação:

- superfície de vidro padrão e suas variações de densidade;
- fundo atmosférico;
- reflexo interno;
- destaque de hover/focus;
- apresentação de modal e backdrop;
- tratamento de tabelas, inputs, menus e alertas;
- fallback opaco via `@supports`.

Um novo `motion.js` centralizará comportamento visual progressivo:

- revelar grupos de elementos com atraso curto e limitado;
- marcar conteúdo que entrou no viewport;
- aplicar estado de página pronta;
- encerrar estados de carregamento visual;
- não executar animações não essenciais quando o usuário prefere movimento
  reduzido.

`dashboard.css` e `login.css` continuarão responsáveis apenas por composição e
particularidades de suas telas. IDs e atributos usados pelos scripts atuais não
serão renomeados. Ajustes de HTML serão limitados a wrappers e classes de
apresentação necessários para a nova hierarquia.

## Aplicação por tela

### Dashboard

- Sidebar ligeiramente destacada do fundo e com vidro denso.
- Cabeçalho e controles com agrupamento mais claro e espaço respirável.
- Resumo financeiro reorganizado como faixa de cards de leitura rápida.
- Gráficos e tabelas em uma grade mais equilibrada, mantendo a ordem funcional
  e a responsividade existentes.
- Item de navegação ativo com indicador deslizante e brilho menta controlado.
- Linhas de tabela com realce leve, sem deslocamento que prejudique leitura.
- Modais com superfície densa, escala curta e blur do plano inferior.

### Autenticação

- Login, cadastro e verificação permanecem no mesmo fluxo deslizante.
- O contêiner principal torna-se uma superfície de vidro denso sobre fundo
  atmosférico.
- O painel lateral preserva o roxo-ardósia, agora com transparência e reflexos
  compatíveis com a linguagem global.
- Inputs ganham profundidade sutil e foco menta claramente visível.
- A animação atual entre login e cadastro será refinada, não substituída por um
  fluxo novo.

### Landing e loading

- A landing mínima recebe a mesma atmosfera e uma superfície de apresentação
  coerente com autenticação e dashboard.
- O loading preserva a animação da marca e passa a usar blur/nitidez para
  conectar visualmente a espera à entrada da próxima tela.

## Movimento

### Entrada

- A página parte de opacidade reduzida e blur leve, tornando-se nítida em uma
  animação curta.
- Grupos de cards entram em sequência com diferença pequena entre itens.
- A sequência possui limite de duração; ela nunca bloqueia interação nem cresce
  indefinidamente com listas longas.

### Interação

- Cards interativos elevam aproximadamente 4 px no hover, com ajuste de borda e
  reflexo.
- Botões comprimem levemente no estado ativo.
- Abas, filtros e navegação usam indicador deslizante quando a estrutura atual
  permitir transição sem afetar semântica.
- Modais usam escala e opacidade curtas, com fechamento visualmente simétrico.
- Números e gráficos podem ganhar revelação progressiva após os dados estarem
  disponíveis; o valor final e a informação textual continuam imediatamente
  acessíveis.

As animações priorizam `transform` e `opacity`. Blur animado será usado apenas
em áreas controladas e por duração curta. Nenhuma animação contínua será
aplicada a cards, tabelas ou conteúdo financeiro.

## Responsividade e desempenho

- Em telas pequenas, blur, sombras e deslocamentos terão intensidade reduzida.
- O layout continuará funcional sem hover.
- Elementos decorativos não poderão provocar rolagem horizontal.
- Listas longas não receberão animação individual ilimitada.
- `will-change` só será aplicado durante estados animados, evitando promoção
  permanente de muitas camadas.
- O fallback sem `backdrop-filter` usará superfícies opacas derivadas dos tokens
  de cada tema, mantendo contraste e hierarquia.

## Acessibilidade

- `prefers-reduced-motion: reduce` remove entradas sequenciais, movimentos de
  fundo, elevação e deslocamentos; transições essenciais tornam-se imediatas ou
  quase imediatas.
- Estados de foco por teclado permanecem mais evidentes que os reflexos
  decorativos.
- Transparência nunca será o único meio de distinguir uma superfície.
- Textos, números, badges e mensagens de erro manterão contraste suficiente nos
  dois temas.
- Estados de erro, vazio e carregamento terão texto ou semântica equivalente,
  não apenas animação.

## Tratamento de erros e compatibilidade

- A ausência de suporte a blur não gera falha de layout.
- JavaScript de movimento é melhoria progressiva: se não carregar, todo o
  conteúdo permanece visível e utilizável.
- O script não interfere em chamadas de API, CSRF, submissões de formulário ou
  listeners funcionais existentes.
- Erros de rede continuam usando os componentes atuais de alerta, apenas com a
  apresentação visual atualizada.

## Validação

- Executar a suíte Maven existente.
- Preservar e executar os testes de marcação do dashboard.
- Adicionar testes de marcação somente para recursos estruturais essenciais,
  como carregamento dos novos arquivos compartilhados e classes-base.
- Validar manualmente login, cadastro, verificação, navegação, filtros, modais e
  ações do dashboard.
- Inspecionar tema claro e escuro em desktop e viewport móvel.
- Verificar navegação por teclado, foco visível, contraste, fallback sem blur e
  `prefers-reduced-motion`.
- Confirmar ausência de erros no console e de overflow horizontal.

## Critérios de aceite

- Todas as telas apresentam a mesma linguagem de liquid glass sem abandonar a
  paleta Rendo.
- Tema claro e escuro têm acabamento equivalente.
- A reorganização melhora a hierarquia sem remover informação ou alterar o
  fluxo de uso.
- Animações são suaves, discretas, não bloqueiam interação e respeitam redução
  de movimento.
- O sistema continua funcional quando blur ou JavaScript de movimento não
  estiverem disponíveis.
- Testes automatizados e validações visuais passam sem regressões funcionais.
