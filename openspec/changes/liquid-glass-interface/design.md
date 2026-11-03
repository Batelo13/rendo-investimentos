## Context

Ver `proposal.md` — Why. O frontend é Thymeleaf com Bootstrap 5.3.3 apenas
como CSS, JavaScript vanilla, SVGs inline e nenhum bundler. `tokens.css` já
centraliza tema, cor, espaçamento e raio; `dashboard.css`, `login.css` e
`loading.css` concentram composição; `theme.js` aplica a preferência antes do
paint; `loading.js`, `login.js` e `dashboard.js` controlam seus fluxos atuais.

O Graphify foi atualizado antes deste design (`graphify update .`) e confirmou
como núcleo de impacto os tokens, os CSS de tela, os templates, `theme.js` e a
capacidade `loading-screen`. A implementação deve preservar IDs/classes lidos
pelos scripts atuais e não criar dependência de build.

## Goals / Non-Goals

**Goals:**

- Introduzir primitives visuais compartilhadas sem transformar `dashboard.css`
  ou `login.css` em uma segunda fonte de tokens.
- Manter o HTML utilizável antes e sem a execução do script de movimento.
- Dar paridade de acabamento a claro e escuro com valores específicos por tema.
- Separar comportamento decorativo de comportamento funcional existente.
- Permitir validação automatizada da presença e integração dos recursos.

**Non-Goals:**

- Adicionar framework CSS/JS, biblioteca de animação ou bundler.
- Reescrever gráficos, chamadas de API, formulários ou estado do dashboard.
- Criar paralaxe de cursor, partículas, WebGL ou física de vidro.
- Mover conteúdo entre rotas ou alterar permissões e fluxos de autenticação.

## Decisions

### Tokens semânticos em `tokens.css`

Serão adicionados tokens para fundo atmosférico, três densidades de superfície,
borda, brilho interno, sombra, blur, durações e curvas. Cada tema define seus
próprios valores, enquanto menta, sucesso, alerta e erro continuam semânticos e
fixos.

**Alternativa descartada:** valores literais em cada CSS de tela. Isso tornaria
claro/escuro divergentes e repetiria ajustes de contraste.

### Primitive compartilhada em `glass.css`

`glass.css` será carregado depois de `tokens.css` e antes dos estilos específicos
de cada tela. Ele conterá apenas superfícies, atmosfera, estados compartilhados,
fallback `@supports` e regras globais de redução de movimento. Layout de
dashboard e autenticação permanece nos arquivos existentes.

**Alternativa descartada:** colocar todas as regras em `base.css`. `base.css`
hoje é pequeno e estável; misturar primitives complexas com base/logo apagaria
sua responsabilidade.

### Movimento como melhoria progressiva

O HTML nasce visível. `motion.js` adiciona uma classe de capacidade ao `<html>`
e só então elementos explicitamente marcados podem usar entrada sequencial.
`IntersectionObserver` ativa grupos abaixo da dobra; um índice limitado impede
atraso crescente em listas longas. O script não observa nem intercepta
submissões, chamadas de API ou navegação.

**Alternativa descartada:** iniciar conteúdo com `opacity: 0` apenas por CSS.
Uma falha no script deixaria a tela invisível.

### Loading e movimento usam um único estado de página pronta

`loading.js` continua decidindo quando o overlay aparece e sai. Ao concluir, ele
marca a página como pronta; `motion.js` reage somente à mesma classe sem duplicar
listeners de `load` ou alterar o envio de formulários.

**Alternativa descartada:** temporizadores independentes para loading e entrada,
que poderiam produzir sobreposição ou tela bloqueada em rede lenta.

### Tema aplicado antes do paint em todas as páginas

O snippet antecipado de tema será reutilizado em autenticação e landing antes
dos stylesheets. `theme.js` permanece responsável por alternância/persistência
no dashboard. O login passa a consumir neutros temáticos, mas os accents menta e
roxo-ardósia permanecem fixos.

**Alternativa descartada:** manter autenticação com paleta única. Isso viola a
paridade aprovada entre temas e contradiz o spec global de persistência.

### Fallback em duas camadas

Tokens definem cores opacas equivalentes; `@supports (backdrop-filter: ...)`
habilita transparência e blur apenas quando disponíveis. A hierarquia visual
não depende do blur para existir.

**Alternativa descartada:** detectar suporte em JavaScript. CSS já oferece
detecção declarativa sem criar flash ou dependência do script.

### Reorganização moderada preserva contratos de DOM

Wrappers e classes de apresentação podem ser adicionados, mas IDs, nomes de
campos, `data-*`, regiões atualizadas por `innerHTML` e seletores usados pelo
JavaScript permanecem estáveis. Elementos renderizados dinamicamente recebem
classes glass nos helpers existentes somente quando necessário.

**Alternativa descartada:** reconstruir templates e scripts como componentes.
O benefício visual não justifica ampliar o escopo para uma migração de frontend.

## Risks / Trade-offs

- [Blur e sombras podem custar GPU em hardware modesto] → Limitar blur a
  superfícies estruturais, reduzir intensidade no mobile e animar principalmente
  `transform`/`opacity`.
- [Transparência reduz contraste em fundos variáveis] → Combinar tint, borda,
  sombra e brilho interno; validar texto e foco nos dois temas.
- [Reorganização pode quebrar seletores do JavaScript] → Preservar contratos de
  DOM e ampliar testes de marcação antes de mudar templates.
- [Animações sequenciais podem atrasar listas] → Aplicar somente em grupos
  curtos e limitar o índice de stagger.
- [Tema antecipado duplicado entre templates pode divergir] → Usar o mesmo
  snippet mínimo e cobrir a presença com testes de marcação.
- [Fallback sem blur pode parecer menos rico] → Aceito; compatibilidade e
  legibilidade têm prioridade sobre fidelidade estética.

## Migration Plan

1. Introduzir testes de marcação e os recursos compartilhados sem mudar layout.
2. Integrar tokens/glass/motion em landing e loading como prova de infraestrutura.
3. Migrar autenticação e validar os dois temas e o fluxo deslizante.
4. Migrar dashboard por regiões, preservando seletores e comportamento.
5. Executar testes automatizados, validação OpenSpec e inspeção visual/funcional.
6. Atualizar Graphify após o lote final e consultar impacto inesperado.

Não há migração de dados. Rollback consiste em reverter os commits de
apresentação; APIs e banco permanecem compatíveis durante todo o processo.
