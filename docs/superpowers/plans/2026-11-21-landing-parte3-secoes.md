# Landing parte 3: problema, tour, diferenciais, segurança e chamada final (plano de implementação)

> **Para agentes:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans para executar este plano tarefa por tarefa. Os passos usam checkbox (`- [ ]`) para acompanhamento.

**Objetivo:** substituir as seções de texto estático depois do simulador por seções que **mostram** o produto:
- o problema que se resolve na tela;
- um tour com réplicas das telas reais;
- diferenciais com microdemonstrações;
- segurança com o que existe de fato;
- chamada final com contador e botão magnético.

**Arquitetura:**
- Cada seção é HTML no `index.html` com CSS em `landing.css`.
- Comportamento genérico vira atributo no `motion.js`: `data-resolve` (estado antes → depois ao entrar na tela) e `data-ima` (botão magnético).
- Comportamento específico fica em dois arquivos novos: `landing-tour.js` (passo ativo do tour) e `landing-diferenciais.js` (câmbio e histórico).
- A seção "Acompanhamento" sai: o passo 4 do tour mostra o dashboard.

**Stack:** Thymeleaf, JS puro, CSS, SVG. Testes: JUnit 5 (`LiquidGlassMarkupTest`) e Playwright Python (`src/test/playwright/landing_regression.py`).

**Spec:** `docs/superpowers/specs/2026-11-20-landing-experiencia-imersiva-design.md`, seções 0 (links do cabeçalho), 2, 4, 5, 6 e 7, mais "Sistema de movimento", "Verificação" e "Entrega/parte 3".

## Restrições globais

- CSP `script-src 'self'`: nada de `<script>` inline nem `on*=`. Nenhuma dependência nova. Nada vai ao servidor.
- Todo número é demonstrativo e rotulado no próprio bloco ("Dados demonstrativos", "Cotação ilustrativa" ou "exemplo").
- As réplicas das telas são `aria-hidden="true"`; o texto de cada passo descreve a tela.
- Na lista de segurança entram **só proteções que existem**: verificação por e-mail, limite de tentativas de login, CPF protegido, senha com BCrypt, dados isolados por conta, cabeçalhos de segurança/CSP e gate de segurança semanal. A frase "Nenhum dinheiro real circula na Rendo." é obrigatória.
- `prefers-reduced-motion: reduce`: estados finais na hora, sem `data-ima` e sem inclinação.
- `data-ima`, `data-luz` e `data-inclina` só com `(hover: hover) and (pointer: fine)`.
- Sem neon; blocos de tamanhos diferentes (nada de grade de cards iguais).
- Commits pequenos em português, na `27-develop`, com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Depois de alterar código: `graphify update .`.

## Decisões deste plano (registradas na change OpenSpec)

1. **Diferenciais em 3 blocos** (câmbio, histórico de cotação, preço médio). A "confirmação de ordem" fica só no passo 3 do tour, para não repetir a mesma réplica (o design permite "três ou quatro").
2. **Os 3 recursos que saem dos blocos** (histórico de operações, evolução patrimonial, tema claro/escuro) viram uma linha "Também na Rendo:" abaixo dos blocos, para não perder informação.
3. **Seção "Acompanhamento" removida:** o passo 4 do tour mostra o mesmo conteúdo.
4. **Cabeçalho:** A Rendo, Experimente, Como funciona e Segurança, sem "Recursos" (seção 0 do design). O rodapé fica como está.
5. **Tour sem mover DOM:** cada passo é um `article` com texto e réplica. No desktop, a réplica é `position: sticky` dentro do próprio passo, e um índice fixo à esquerda marca o passo ativo. No celular, os passos empilham sem sticky (stepper natural).

## Foco de revisão

1. **Clicar num passo do tour no meio do scroll de outro:** a página vai ao passo clicado e só ele fica ativo. → `tour_clique_leva_ao_passo` (Task 2).
2. **Celular de 390 px:** nada de scroll horizontal no tour e nos diferenciais, e a réplica não fica grudada cobrindo o texto. → `tour_celular_sem_sticky` (Task 2) e `diferenciais_sem_overflow_no_celular` (Task 3).
3. **Clicar várias vezes rápido no botão de câmbio:** termina na moeda do último estado, sem número intermediário parado. → `cambio_cliques_rapidos` (Task 3).
4. **Toque no botão magnético:** não desloca o botão. → `cta_botao_magnetico` (Task 4, contexto de toque).
5. **Movimento reduzido:** problema já resolvido, tour com todas as réplicas visíveis e câmbio sem animação. → `problema_movimento_reduzido` (Task 1) e `diferenciais_movimento_reduzido` (Task 3).

---

### Task 1: Change OpenSpec + seção "O problema" + `data-resolve`

**Arquivos:**
- Criar: `openspec/changes/landing-secoes-demonstrativas/{proposal.md,design.md,tasks.md,specs/pagina-inicial-imersiva/spec.md}`
- Modificar: `src/main/resources/static/js/motion.js` (`data-resolve`)
- Modificar: `src/main/resources/templates/index.html` (seção `#problema` entre `#visao-geral` e `#experimente`)
- Modificar: `src/main/resources/static/css/landing.css`
- Teste: `LiquidGlassMarkupTest`, `landing_regression.py`

**Interfaces:**
- Produz o atributo `data-resolve` (`motion.js`).
  - Ao ficar 60% visível (IntersectionObserver), espera 600 ms e adiciona a classe `resolvido`.
  - Com movimento reduzido ou sem IntersectionObserver, adiciona `resolvido` na hora.
  - O CSS mostra `.antes`, e com `.resolvido` troca para `.depois` (opacidade e `transform`).
  - `:hover` sobre um card ainda não resolvido também mostra `.depois`.

**Conteúdo da seção** (`<section class="lp-secao lp-problema" id="problema" data-motion>`):
- Sobretítulo "O problema" e título "Por que começar a investir trava tanta gente".
- Três `article.problema-card[data-resolve]`, cada um com `h3`, `div.antes` e `div.depois` (os dois sempre no DOM):

| h3 | .antes | .depois |
|---|---|---|
| Medo de errar com dinheiro real | "Seu dinheiro em risco a cada decisão" | "R$ 0 em risco" + "R$ 100.000 de saldo fictício para praticar" |
| Informação espalhada | 4 pílulas soltas: "Corretora A", "Corretora B", "planilha", "câmbio do dia" | uma linha: "PETR4 · 300 ações · preço médio R$ 31,20 · 2 corretoras" |
| Não saber se está indo bem | "?" grande + "Lucro ou prejuízo?" | "Realizado + R$ 420" e "Não realizado − R$ 85", lado a lado |

- Rótulo "Valores de exemplo" no rodapé da seção.

- [ ] **Passo 1:** criar a change OpenSpec `landing-secoes-demonstrativas`.
  - **proposal:**
    - endpoints e banco: nenhum;
    - BREAKING: nenhum.
  - **design:**
    - as 5 decisões acima;
    - STRIDE: sem entrada no servidor; a lista de segurança só com proteções reais (evita "information disclosure" enganosa).
  - **delta ADDED em `pagina-inicial-imersiva`:**
    - "O problema que a Rendo resolve";
    - "Tour do produto";
    - "Diferenciais demonstrados";
    - "Segurança verificável";
    - "Chamada final com contador";
    - cada um com os cenários das Tasks 1 a 4.
  - **delta REMOVED:** nenhum. O requirement "Conteúdo fiel ao produto" continua valendo.
  - **tasks** citando os testes.
  - Validar com `openspec validate landing-secoes-demonstrativas --strict` → `is valid`.
- [ ] **Passo 2: testes que falham.**
  - `LiquidGlassMarkupTest.landingTemSecaoProblema`:
    - `index.html` contém `id="problema"`;
    - `data-resolve` aparece 3 vezes;
    - `indexOf("id=\"problema\"")` fica entre `id="visao-geral"` e `id="experimente"`;
    - `motion.js` contém `data-resolve`.
  - Playwright:
    - `problema_resolve_ao_entrar`, em 1280×900:
      - antes de rolar, nenhum `.problema-card` tem a classe `resolvido`;
      - depois de `#problema` `scrollIntoView({block:'center'})` e 1,4 s, os 3 têm `resolvido`, e o `.depois` de cada um tem opacidade > 0,9.
    - `problema_movimento_reduzido`: no contexto `reduced_motion="reduce"`, logo após o load os 3 já têm `resolvido`.
- [ ] **Passo 3: rodar e ver falhar.**
  - `./mvnw test -Dtest=LiquidGlassMarkupTest`
  - Subir a instância descartável: `./mvnw -q -B package -DskipTests && java -jar target/gestao-investimentos-0.0.1-SNAPSHOT.jar --server.port=8081 "--spring.datasource.url=jdbc:h2:mem:landing;DB_CLOSE_DELAY=-1" --spring.jpa.hibernate.ddl-auto=create-drop --app.mail.enabled=false --app.admin.emails= &`
  - Rodar `RENDO_URL=http://localhost:8081 python src/test/playwright/landing_regression.py` → FAIL nos novos.
- [ ] **Passo 4:** implementar `data-resolve`, a seção e o CSS.
  - **Layout:** cards em grade `1.2fr 1fr 1fr` no desktop (tamanhos diferentes) e 1 coluna abaixo de 1000 px.
  - **Cores:** "+" em `--lp-esmeralda-claro`, "−" em `#E8A48F` (suave, sem vermelho saturado).
- [ ] **Passo 5: rodar e ver passar** (Java e Playwright, incluindo os 33 testes anteriores).
- [ ] **Passo 6: commit** `feat(landing): secao O problema com estados antes e depois`.

---

### Task 2: Tour "Como funciona" com réplicas das telas

**Arquivos:**
- Modificar: `src/main/resources/templates/index.html`:
  - reescrever `#como-funciona`;
  - remover a `section.lp-acompanhamento` inteira;
  - adicionar `<script th:src="@{/js/landing-tour.js}"></script>` depois do `landing-simulador.js`.
- Criar: `src/main/resources/static/js/landing-tour.js`
- Modificar: `src/main/resources/static/css/landing.css` (sai o CSS de `.lp-passos`, `.lp-acompanhamento*` e `.lp-ilustracao` sem uso; entra o do tour)
- Teste: `LiquidGlassMarkupTest`, `landing_regression.py`

**Interfaces:**
- Produz:
  - `nav.tour-indice` com 4 `a[href="#tour-N"]` (N = 1..4);
  - 4 `article.tour-passo#tour-N`, cada um com `div.tour-texto` (número, `h3`, `p`) e `figure.tour-tela.glass-surface-strong[aria-hidden="true"]`.
- `landing-tour.js`:
  - IntersectionObserver sobre os 4 artigos com `rootMargin: '-45% 0px -50% 0px'`;
  - o artigo no meio da tela ganha a classe `ativo`, e o link correspondente do índice ganha `aria-current="true"`; os demais perdem os dois;
  - clique no índice: deixa o navegador seguir a âncora, com `scroll-margin-top` no CSS para não ficar sob o cabeçalho;
  - com movimento reduzido, todos os artigos recebem `ativo` de uma vez.

**Réplicas** (HTML/CSS com tokens e `glass.css`; elementos internos animam por CSS quando o artigo tem `.ativo`). Mesmos textos dos 4 passos atuais.

| Passo | Réplica (rótulo "Tela ilustrativa" em cada uma) |
|---|---|
| 1 Crie sua conta | campos Nome "Ana Souza", E-mail "ana@exemplo.com", CPF "•••.•••.•••-••", e 6 caixas de código "4 8 2 9 1 7" que acendem em sequência (40 ms de atraso cada) |
| 2 Cadastre corretoras | campo CNPJ "12.345.678/0001-90 (exemplo)", nome "Corretora Exemplo S.A." e selo "Conferido na CVM" que aparece com `scale(0.9→1)` |
| 3 Compre e venda ações | modal "Revise sua ordem": Compra · PETR4 · 100 ações · R$ 38,50 · Total R$ 3.850,00 · Corretora Exemplo; botões "Cancelar" e "Confirmar" (visuais) |
| 4 Acompanhe a evolução | mini-dashboard: Patrimônio R$ 103.240, 3 linhas de posição (PETR4, VALE3, AAPL) e uma polyline de evolução que se desenha (`stroke-dashoffset`) |

**Layout:**
- **Desktop (≥ 1000 px):** grade `220px 1fr`.
  - Índice `position: sticky; top: 120px` à esquerda.
  - Cada `article` com `min-height: 80vh` e grade `minmax(0,1fr) minmax(0,1.1fr)`.
  - A `figure` é `position: sticky; top: 18vh`.
- **< 1000 px:** índice oculto; artigos empilhados, `figure` abaixo do texto e `position: static`.

- [ ] **Passo 1: testes que falham.**
  - `LiquidGlassMarkupTest.landingTemTourDoProduto`:
    - `class="tour-passo"` aparece 4 vezes;
    - `id="tour-1"` … `id="tour-4"` existem;
    - `Conferido na CVM` e `Revise sua ordem` existem;
    - `lp-acompanhamento` não existe;
    - `@{/js/landing-tour.js}` vem depois de `@{/js/landing-simulador.js}`.
  - Playwright:
    - `tour_marca_passo_ao_rolar`: `#tour-3` `scrollIntoView({block:'center'})` e 0,8 s → `#tour-3` tem `ativo`, o link `a[href="#tour-3"]` tem `aria-current="true"`, e nenhum outro artigo tem `ativo`.
    - `tour_clique_leva_ao_passo`: com `#tour-1` centralizado, clicar em `a[href="#tour-4"]` e esperar 1,2 s → `#tour-4` tem `ativo` e o topo dele está dentro da viewport.
    - `tour_celular_sem_sticky`: em 390×844, `getComputedStyle(figure).position` é `static` nos 4, e `scrollWidth <= innerWidth`.
    - `tour_movimento_reduzido`: os 4 artigos têm `ativo` logo após o load.
    - `screenshots_tour`: `#como-funciona` em 1280 e 390 → `target/landing-screens/tour-<largura>.png`.
- [ ] **Passo 2: rodar e ver falhar.**
- [ ] **Passo 3: implementar** markup, `landing-tour.js` e CSS. Remover o CSS que ficou sem uso (`grep` por `lp-passos`, `lp-acompanhamento`, `lp-ilustracao` em `index.html` → zero resultados antes de apagar).
- [ ] **Passo 4: rodar e ver passar;** abrir `tour-*.png` e checar se a réplica não cobre texto e se os rótulos estão legíveis.
- [ ] **Passo 5: commit** `feat(landing): tour do produto com replicas das telas reais`.

---

### Task 3: Diferenciais que se mostram + cabeçalho

**Arquivos:**
- Modificar: `src/main/resources/templates/index.html`:
  - reescrever `#recursos`;
  - tirar `<a href="#recursos">Recursos</a>` do **cabeçalho** (o rodapé mantém);
  - adicionar `<script th:src="@{/js/landing-diferenciais.js}"></script>` depois do `landing-tour.js`.
- Criar: `src/main/resources/static/js/landing-diferenciais.js`
- Modificar: `src/main/resources/static/css/landing.css`
- Teste: `LiquidGlassMarkupTest`, `landing_regression.py`

**Interfaces:**
- Consome: `window.RendoMotion.formatar(v, 'brl')` e `window.RendoMotion.reduzMovimento`; os atributos `data-contador`, `data-luz` e `data-inclina` (parte 1).
- Produz (DOM estável):
  - `#difCambioBotao` (`button`, `aria-pressed`) e `#difCambioValor`;
  - `#difHistorico` (`input type=range`, min 0, max 29, valor inicial 29), com rótulo "Percorrer a cotação" e `#difHistoricoTexto`;
  - `#difPrecoMedio` (`[data-contador="32" data-formato="brl"]`).

**Blocos** (`div.dif-grade` com 3 `article.dif-bloco[data-luz][data-inclina="3"]` de tamanhos diferentes: câmbio 1 coluna, histórico 2 colunas, preço médio 1 coluna em 2 linhas):

1. **Câmbio:** "AAPL · 10 ações · US$ 228,00".
   - O valor começa em "US$ 2.280,00".
   - O botão "Ver em reais" / "Ver em dólares" alterna entre "R$ 12.312,00" e "US$ 2.280,00", contando do valor exibido até o novo em 600 ms. Cliques no meio da contagem recomeçam a partir do valor exibido; o fim é sempre exato.
   - Rótulo "US$ 1 = R$ 5,40 (cotação demonstrativa)".
   - Formatação em dólar: `Intl.NumberFormat('pt-BR', {style:'currency', currency:'USD'})`.
2. **Histórico de cotação:** SVG com 30 pontos, `v(i) = 38.5 + 1.6 * Math.sin(i / 4.2) + 0.05 * i`, i = 0..29.
   - O `input type=range` fica sobre o gráfico (largura total, opaco 0, mas focável e com foco visível no contorno do bloco).
   - Um marcador e o texto `#difHistoricoTexto` = `"dia <i+1> · R$ <v>"` (formato `brl`).
   - Rótulo "Cotação ilustrativa".
3. **Preço médio:** linhas "100 ações a R$ 30,00" e "50 ações a R$ 36,00", e "Preço médio" com `<strong id="difPrecoMedio" data-contador="32" data-formato="brl">R$ 32,00</strong>`. Rótulo "Valores de exemplo".

- Abaixo dos blocos: `p.dif-tambem` "Também na Rendo: histórico de operações com cancelamento, evolução patrimonial de 1 dia a todo o período e tema claro e escuro na área logada."

- [ ] **Passo 1: testes que falham.**
  - `LiquidGlassMarkupTest.landingTemDiferenciaisDemonstrados`:
    - `class="dif-bloco` aparece 3 vezes;
    - `Cotação ilustrativa`, `Valores de exemplo` e `Também na Rendo:` existem;
    - o `nav.lp-nav` do cabeçalho não contém `href="#recursos"` (recortar o trecho entre `<nav class="lp-nav"` e `</nav>`);
    - `@{/js/landing-diferenciais.js}` vem depois de `@{/js/landing-tour.js}`.
  - Playwright:
    - `cambio_alterna_moeda`: clicar `#difCambioBotao` e esperar 0,9 s → `"R$ 12.312,00"`; clicar de novo, 0,9 s → `"US$ 2.280,00"`.
    - `cambio_cliques_rapidos`: 3 cliques com 80 ms entre eles e espera de 0,9 s → `"R$ 12.312,00"` (número ímpar de cliques).
    - `historico_percorre_cotacao`: definir valor 0 e disparar `input` → `#difHistoricoTexto` começa com `"dia 1 · R$ 38,50"`; focar e apertar `End` → começa com `"dia 30 · R$"`.
    - `preco_medio_conta`: rolar até `#difPrecoMedio` e esperar 1,6 s → `"R$ 32,00"`.
    - `diferenciais_sem_overflow_no_celular`: em 390×844, `scrollWidth <= innerWidth` com `#recursos` visível.
    - `diferenciais_movimento_reduzido`: no contexto reduzido, clicar no câmbio e esperar 50 ms → já `"R$ 12.312,00"`.
- [ ] **Passo 2: rodar e ver falhar.**
- [ ] **Passo 3: implementar.**
- [ ] **Passo 4: rodar e ver passar.**
  - Ajustar `cabecalho_marca_secao_ativa` só se ele depender de "Recursos". Hoje usa `#como-funciona`, então não deve mudar.
  - Screenshots `diferenciais-<largura>.png` (1280 e 390) via uma função `screenshots_diferenciais`.
- [ ] **Passo 5: commit** `feat(landing): diferenciais com microdemonstracoes de cambio, historico e preco medio`.

---

### Task 4: Segurança verificável + chamada final com `data-ima`

**Arquivos:**
- Modificar: `src/main/resources/templates/index.html` (`#seguranca` e `section.lp-cta`)
- Modificar: `src/main/resources/static/js/motion.js` (`data-ima`)
- Modificar: `src/main/resources/static/css/glass.css` (regra de `[data-ima]`) e `landing.css`
- Teste: `LiquidGlassMarkupTest`, `landing_regression.py`

**Interfaces:**
- Produz o atributo `data-ima` (`motion.js`).
  - Em `pointermove` sobre o elemento (rAF), escreve `--ima-x`/`--ima-y` = deslocamento do ponteiro em relação ao centro × 0,25, limitado a ±6 px.
  - Em `pointerleave`, volta a `0px`.
  - CSS: `[data-ima] { translate: var(--ima-x, 0) var(--ima-y, 0); transition: translate 0.3s cubic-bezier(0.16,1,0.3,1); }`.
  - Ligado só nas mesmas condições de `data-luz`.

**Segurança:**
- Título "Por que confiar".
- Frase em destaque: "Nenhum dinheiro real circula na Rendo."
- Lista `ul.lp-seguranca-lista` com 7 itens, cada `li` com `data-motion` (aparece em sequência):

| Item | Texto |
|---|---|
| E-mail verificado | A conta só é ativada com o código enviado ao seu e-mail. |
| Limite de tentativas | Após 5 senhas erradas, o login fica bloqueado por 15 minutos. |
| Senha protegida | Armazenada apenas como hash BCrypt, nunca em texto puro. |
| CPF não exposto | Guardado só como resumo criptográfico com chave secreta e nunca devolvido pela API. |
| Dados isolados | Cada conta acessa apenas a própria carteira, posições e operações. |
| Cabeçalhos de segurança | Política de conteúdo restrita, sem scripts de terceiros. |
| Verificação semanal | Testes automáticos de segurança rodam toda semana. |

**Chamada final:**
- `h2` "Comece com <span data-contador="100000" data-formato="brl0">R$ 100.000</span> fictícios."
- O `p` atual.
- Botão primário "Criar minha conta" com `data-ima`; o secundário "Já tenho conta" fica.

- [ ] **Passo 1: testes que falham.**
  - `LiquidGlassMarkupTest.landingTemSegurancaECtaFinal`:
    - `#seguranca` tem 7 `<li`, "Nenhum dinheiro real circula na Rendo." e "Limite de tentativas";
    - o CTA tem `data-contador="100000"` e `data-ima`;
    - `motion.js` contém `data-ima`.
  - Playwright `cta_botao_magnetico`:
    - em 1280×900, com o CTA visível, mover o mouse para 10 px à direita do centro do botão e esperar 0,3 s → `--ima-x` diferente de `0px` e com no máximo 6 px em módulo;
    - num contexto `has_touch=True, is_mobile=True`, tocar no botão → `--ima-x` vazio.
- [ ] **Passo 2: rodar e ver falhar.**
- [ ] **Passo 3: implementar.**
- [ ] **Passo 4: rodar e ver passar.**
- [ ] **Passo 5: commit** `feat(landing): seguranca verificavel e chamada final com botao magnetico`.

---

### Task 5: Verificação final, arquivamento e PR

- [ ] **Passo 1:** `./mvnw test` → `Failures: 0`.
- [ ] **Passo 2:** `landing_regression.py` completo na 8081 → todos passam. `desempenho_scroll` com CLS < 0,1 e no máximo 1 tarefa longa.
- [ ] **Passo 3:** abrir as screenshots novas (`tour-*`, `diferenciais-*`) e uma captura de página inteira em 1280 e 390 (`page.screenshot(full_page=True)` → `target/landing-screens/pagina-<largura>.png`) para checar ritmo e espaçamento entre seções.
- [ ] **Passo 4:** `bash scripts/security_gate.sh` → `APROVADO`; `bash scripts/dast_zap.sh` → `APROVADO`.
- [ ] **Passo 5:**
  - marcar as tasks da change;
  - `openspec archive landing-secoes-demonstrativas -y`;
  - commit `docs(openspec): arquiva landing-secoes-demonstrativas` com "Endpoints alterados: nenhum";
  - `graphify update .`.
- [ ] **Passo 6:** o push e o PR único das partes 1 a 3 para a `main` só acontecem quando o usuário confirmar.
