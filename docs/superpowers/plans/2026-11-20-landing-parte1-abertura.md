# Landing parte 1: sistema de movimento e abertura "carteira ganhando forma" (plano de implementação)

> **Para agentes:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans para executar este plano tarefa por tarefa. Os passos usam checkbox (`- [ ]`) para acompanhamento.

**Objetivo:** trocar a cena do boneco/barras/cidade por um cartão de carteira demonstrativa que ganha forma com o scroll e reage ao ponteiro. Criar o sistema de movimento reutilizável (contador, luz, inclinação) e remover o código de vídeo.

**Arquitetura:** o motor de progresso do `landing.js` (p de 0 a 1 interpolado em rAF, momentos de texto, foco por teclado) continua. Só a função de desenho da cena é trocada. O cartão é HTML em vidro (para ter perspectiva 3D em CSS), com SVG dentro para gráfico e anel. O `motion.js` ganha comportamentos por atributo `data-*`, sem dependência nova.

**Stack:** Thymeleaf, JS puro (ES2020, sem build), CSS, SVG, Web Animations API, IntersectionObserver. Testes: JUnit 5 (contratos de markup/HTTP) e Playwright Python (`src/test/playwright/`, padrão do `auth_focus_regression.py`).

**Spec:** `docs/superpowers/specs/2026-11-20-landing-experiencia-imersiva-design.md` (seções 0, 1, 1b, "Sistema de movimento", SEO, Segurança, Verificação e Entrega/parte 1).

## Restrições globais

- CSP `script-src 'self'`: nada de `<script>` inline nem atributo `on*=` (o `LiquidGlassMarkupTest` reprova).
- Nenhuma dependência nova (npm, Maven ou vendor).
- Animar só `transform` e `opacity`; `pointermove` agrupado em `requestAnimationFrame`.
- Todo número da cena é demonstrativo e o cartão mostra o rótulo visível **"Dados demonstrativos"**.
- `prefers-reduced-motion: reduce` → cena parada no estado final (p = 1), sem inclinação, contadores no valor final.
- Inclinação e luz só com `(hover: hover) and (pointer: fine)`; sem giroscópio.
- Um único `<h1>` na página (o do momento 01).
- Identidade: tokens de `tokens.css`/`glass.css`, Inter, verde `#5DE2BA`, dourado `#E9C27A`; sem neon (brilho/`filter: blur` de glow ≤ 12px, opacidade ≤ 0.35).
- Commits pequenos em português na `27-develop`, com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Depois de alterar código: `graphify update .`.

## Foco de revisão

1. **Scroll brusco** (âncora "Conhecer a Rendo", tecla End, voltar ao topo): a cena precisa ficar coerente com o p final, sem chip ou fatia pela metade. → Playwright `scroll_brusco_mantem_cena_coerente` (Tarefa 3).
2. **Redimensionar ou girar no meio do scroll** (1280 → 390 px em p ≈ 0,6): nada de scroll horizontal nem cartão cortado. → Playwright `redimensionar_sem_overflow` (Tarefa 3).
3. **Toque** (celular, `pointer: coarse`): tocar a cena não pode inclinar o cartão. → Playwright `toque_nao_inclina` (Tarefa 2).
4. **JS desligado ou falhando:** o cartão e os fatos mostram valores finais legíveis (R$ 100.000,00), sem zeros. → Playwright `sem_js_mostra_valores_finais` (Tarefa 2).
5. **Teclado:** Tab até "Começar agora" e "Construir meu futuro" leva a página até o momento certo, com o botão visível. → Playwright `teclado_foca_botoes_visiveis` (Tarefa 3).

---

### Task 1: Change OpenSpec + remoção do vídeo

**Arquivos:**
- Criar: `openspec/changes/landing-abertura-carteira/{proposal.md,design.md,tasks.md,specs/pagina-inicial-imersiva/spec.md}`
- Modificar: `src/main/java/com/curso/gestaoinvestimentos/security/SecurityConfig.java` (tirar `"/videos/**"` do `permitAll` de GET)
- Modificar: `src/main/resources/templates/index.html` (tirar `<video id="jornadaVideo">`, `data-video`/`data-video-mobile` da `<section id="jornada">`)
- Modificar: `src/main/resources/static/css/landing.css` (tirar `.jornada-video`, `.com-video`)
- Modificar: `src/main/resources/static/js/landing.js` (tirar o bloco "Video", `posicionarVideo`, `carregarVideo` e o listener `rendo:page-ready`; `desenhar` passa a não retornar nada e `passo` só reagenda se `atual !== alvo`)
- Apagar: `src/main/resources/static/images/rendo-jornada-poster.webp`, `src/test/resources/static/videos/teste-publico.mp4`
- Teste: `src/test/java/com/curso/gestaoinvestimentos/SecurityHeadersIntegrationTest.java`, `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`

**Interfaces:**
- Produz: change `landing-abertura-carteira` aberta (as Tarefas 2 a 4 marcam suas tasks nela).

- [ ] **Passo 1: Criar a change OpenSpec.**
  - `proposal.md` (por quê, o que muda, impacto). Endpoint público **removido**: `GET /videos/**` → marcar **BREAKING** (regra do `openspec/config.yaml`).
  - `design.md` com tabela STRIDE: a superfície pública diminui; o resto não muda.
  - Delta da spec `pagina-inicial-imersiva`, depois de ler `openspec/specs/pagina-inicial-imersiva/spec.md`:
    - MODIFIED "Narrativa controlada pelo scroll": sem vídeo, com o cartão de carteira.
    - REMOVED: o cenário "Vídeo indisponível".
    - ADDED: "Cena da carteira demonstrativa", "Profundidade com o ponteiro" e "Contadores animados". Cada requirement descreve os cenários da Tarefa 2/3, sem citar classes Java.
  - `tasks.md`: uma task por tarefa deste plano, cada uma citando o teste.
  - Validar com `openspec validate landing-abertura-carteira --strict` → `is valid`.

- [ ] **Passo 2: Escrever os testes que falham.**
  - Em `SecurityHeadersIntegrationTest`, renomear `videosDaPaginaInicialSaoPublicos` → `assetsDaPaginaInicialSaoPublicos`:
    - manter os asserts `200` de `/js/landing.js`, `/css/landing.css`;
    - trocar o do poster por `/images/favicon.svg` → 200;
    - adicionar `assertThat(get("/videos/qualquer.mp4").statusCode()).isEqualTo(401);`.
  - Em `LiquidGlassMarkupTest`, novo teste `landingNaoTemVideo`:
    - `index.html` não contém `<video`, `data-video` nem `jornada-poster`;
    - `landing.js` não contém `video`.

- [ ] **Passo 3: Rodar e ver falhar.**
  - Rodar `./mvnw test -Dtest='SecurityHeadersIntegrationTest,LiquidGlassMarkupTest'`.
  - Esperado: falham `assetsDaPaginaInicialSaoPublicos` (200 em vez de 401) e `landingNaoTemVideo`.

- [ ] **Passo 4: Remover o vídeo** nos arquivos listados acima. Atualizar também:
  - o comentário de cabeçalho do `landing.js`;
  - o comentário da regra de `permitAll` no `SecurityConfig`: "assets estáticos que ela carrega (CSS/JS/imagens)".

- [ ] **Passo 5: Rodar e ver passar.** Mesmo comando → `BUILD SUCCESS`.

- [ ] **Passo 6: Commit.**
  ```bash
  git add -A openspec/changes/landing-abertura-carteira src/main src/test
  git commit -m "refactor(landing): remove o video da pagina inicial e a rota publica /videos/**"
  ```

---

### Task 2: Sistema de movimento (`data-contador`, `data-luz`, `data-inclina`) + fatos animados

**Arquivos:**
- Modificar: `src/main/resources/static/js/motion.js`
- Modificar: `src/main/resources/static/css/glass.css` (regras de `[data-luz]`, `[data-inclina]` e `[data-contador]`, que são compartilhadas)
- Modificar: `src/main/resources/templates/index.html` (`<dt>R$ 100.000</dt>` da seção `#visao-geral` ganha `data-contador="100000" data-formato="brl0"`)
- Criar: `src/test/playwright/landing_regression.py`
- Teste: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`

**Interfaces:**
- Produz `window.RendoMotion` (definido em `motion.js`, que já carrega antes de `landing.js`):
  - `formatar(valor: number, formato: 'brl' | 'brl0' | 'inteiro'): string`
    - `'brl'` → `Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'})`, exemplo `"R$ 100.000,00"`;
    - `'brl0'` → igual, sem centavos (`"R$ 100.000"`);
    - `'inteiro'` → `"100.000"`.
  - `reduzMovimento: boolean`
- Atributos (contrato HTML usado pelas partes 2 e 3):
  - `data-contador="<número>"` + `data-formato` (padrão `'inteiro'`).
    - O texto renderizado no servidor já é o valor final (SEO e sem JS).
    - Com movimento permitido, ao iniciar, o elemento vai para `formatar(0)`. Ao entrar na tela (mesmo IntersectionObserver de `data-motion`), anima de 0 ao valor em 1200 ms com easeOutCubic, em rAF.
    - Com `reduzMovimento`, não mexe no texto.
  - `data-luz`: em `pointermove` (rAF), escreve `--luz-x`/`--luz-y` em px relativos ao elemento e liga a classe `com-luz`; em `pointerleave`, tira a classe. O CSS desenha um `::after` com `radial-gradient(240px circle at var(--luz-x) var(--luz-y), rgba(255,255,255,0.08), transparent 70%)`, com `pointer-events: none`.
  - `data-inclina="<graus máx>"` (padrão 4) + `data-inclina-area="<seletor>"` opcional (área que escuta o ponteiro; padrão: o próprio elemento).
    - Escreve `--mx`/`--my` (−1..1) **na área** e aplica `transform: perspective(1200px) rotateX(<-my*max>deg) rotateY(<mx*max>deg)` no elemento.
    - Ao sair, volta a 0 com `transition: transform 600ms cubic-bezier(0.16,1,0.3,1)`.
  - `data-luz` e `data-inclina` só são ligados se `matchMedia('(hover: hover) and (pointer: fine)').matches` e não houver `reduzMovimento`.

- [ ] **Passo 1: Escrever o teste de markup que falha.**
  - `LiquidGlassMarkupTest.landingUsaContadorNosFatos`: `index.html` contém `data-contador="100000" data-formato="brl0"`.
  - `motion.js` contém `RendoMotion`, `data-contador`, `data-luz` e `data-inclina`.

- [ ] **Passo 2: Escrever o roteiro Playwright** `src/test/playwright/landing_regression.py`.
  - **Estrutura:**
    - `BASE_URL = os.environ.get("RENDO_URL", "http://localhost:8080")`;
    - funções nomeadas, chamadas por `main()`;
    - falha com `AssertionError` e mensagem em português;
    - coleta erros de console e `securitypolicyviolation` (via `add_init_script` → `console.error`) e os verifica ao final de cada função.
  - **Funções desta tarefa:**
    - `contador_anima_ate_valor_final(page)`: rolar até `#visao-geral` e esperar 1,6 s → o `dt[data-contador]` tem texto `"R$ 100.000"`.
    - `contador_respeita_movimento_reduzido(browser)`: contexto `reduced_motion="reduce"` → o texto é `"R$ 100.000"` logo após o load, sem rolar.
    - `toque_nao_inclina(browser)`: contexto `has_touch=True, is_mobile=True`, viewport 390×844; tocar no `#cenaCarteira` (Tarefa 3; até lá, no `dt` do contador) → `style.transform` inline vazio.
    - `sem_js_mostra_valores_finais(browser)`: `java_script_enabled=False` → o `dt` mostra `"R$ 100.000"`.

- [ ] **Passo 3: Rodar e ver falhar.**
  - Java: `./mvnw test -Dtest=LiquidGlassMarkupTest` → falha `landingUsaContadorNosFatos`.
  - Playwright: subir a instância descartável (comando do passo 6) e rodar `RENDO_URL=http://localhost:8081 python src/test/playwright/landing_regression.py` → `AssertionError` em `contador_anima_ate_valor_final` (o texto não anima, mas já é final; o assert verifica que o texto foi `"R$ 0"` em t ≈ 50 ms após entrar na tela, para provar a animação).

- [ ] **Passo 4: Implementar** `RendoMotion` e os três atributos em `motion.js`, as regras CSS em `glass.css` (`[data-contador] { font-variant-numeric: tabular-nums; }`) e o atributo no `index.html`.

- [ ] **Passo 5: Rodar e ver passar** os dois comandos do passo 3.

- [ ] **Passo 6: Comando da instância descartável** (para este e os próximos roteiros):
  ```bash
  ./mvnw -q -B package -DskipTests && java -jar target/gestao-investimentos-0.0.1-SNAPSHOT.jar --server.port=8081 "--spring.datasource.url=jdbc:h2:mem:landing;DB_CLOSE_DELAY=-1" --spring.jpa.hibernate.ddl-auto=create-drop --app.mail.enabled=false --app.admin.emails= &
  ```

- [ ] **Passo 7: Commit.**
  ```bash
  git add src/main/resources/static/js/motion.js src/main/resources/static/css/glass.css src/main/resources/templates/index.html src/test
  git commit -m "feat(landing): sistema de movimento com contador, luz e inclinacao por atributo"
  ```

---

### Task 3: Abertura "carteira ganhando forma"

**Arquivos:**
- Modificar: `src/main/resources/templates/index.html` (conteúdo de `.jornada-palco`: sai todo o `<svg id="jornadaCena">` antigo e entra a estrutura abaixo)
- Modificar: `src/main/resources/static/js/landing.js` (substituir o IIFE `cena` inteiro; manter momentos, laço, foco, header/dica e barra de progresso)
- Modificar: `src/main/resources/static/css/landing.css` (sai o CSS de estrelas, cidade, pessoa e sol; entra a cena nova, inclusive retrato/celular e movimento reduzido)
- Teste: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`, `src/test/playwright/landing_regression.py`

**Interfaces:**
- Consome: `window.RendoMotion.formatar` (Tarefa 2); atributos `data-inclina`, `data-inclina-area`, `data-luz` (Tarefa 2).
- Produz: IDs estáveis usados pelos testes: `#cenaCarteira`, `#cenaValor`, `#cenaLinha`, `#cenaChips`, `#cenaAnel`, `.cena-satelite`.

**Estrutura** (tudo `aria-hidden="true"`; o texto acessível continua nos momentos):
```
.jornada-palco
├─ svg.jornada-fundo            horizonte em grade (perspectiva), luz de amanhecer
│                               (opacidade dirigida por p); parallax via --mx/--my da seção
└─ .cena-camera                 recebe o transform do scroll (escala/translação do momento 4)
   ├─ .cena-satelite x2         "Posições" (4 linhas: ticker + valor) e "Operações" (3 linhas:
   │                            Compra/Venda + ticker); entram no momento 4; ocultos < 768px
   └─ #cenaCarteira.glass-surface-strong [data-inclina="6" data-inclina-area="#jornada" data-luz]
      ├─ cabeçalho: "Patrimônio" + selo "Dados demonstrativos"
      ├─ #cenaValor               texto renderizado "R$ 100.000,00"
      ├─ svg #cenaGrafico (viewBox 0 0 480 200): g#cenaDegraus (13 barras finas, alturas da
      │   SERIE), polyline#cenaLinha (pathLength="1"), g#cenaMarcadores (compra no índice 1 e 6,
      │   venda no 9; pontos dourados #E9C27A)
      ├─ svg #cenaAnel (viewBox 0 0 120 120): 4 circle.anel-fatia (r=48, pathLength="1")
      └─ ul#cenaChips: 4 li (ticker + %): PETR4 35%, VALE3 25%, WEGE3 20%, AAPL 20%
```

**Dados demonstrativos** (constantes no topo do novo bloco `cena` do `landing.js`):
```js
const ALOCACAO = [['PETR4', 0.35], ['VALE3', 0.25], ['WEGE3', 0.20], ['AAPL', 0.20]];
const SERIE = [100000, 101200, 99800, 102600, 104100, 103000, 105800,
               107200, 106100, 108900, 110300, 109500, 112400]; // ilustrativa, 12 meses
```

**Coreografia** (`atualizar(p)`, reaproveitando `suave`, `misturar` e `limitar` do arquivo):

| Elemento | Regra |
|---|---|
| `#cenaValor` | p < 0,45 → `SERIE[0]`; 0,45–0,75 → valor interpolado em `SERIE` no índice `suave(0.45, 0.75, p) * 12`; depois, `SERIE[12]`; texto via `RendoMotion.formatar(v, 'brl')`, escrito só quando muda |
| chip i | entra entre `0.20 + i*0.04` e `0.28 + i*0.04`: opacidade 0→1, `translate3d(0, 24px→0, 0)` |
| fatia i do anel | `stroke-dasharray = "<pct_i * t> 1"` com `t = suave(0.25 + i*0.04, 0.33 + i*0.04, p)` e `stroke-dashoffset = -(soma das pct anteriores)` |
| `#cenaLinha` | `stroke-dashoffset = 1 - suave(0.45, 0.75, p)` |
| barra k de `#cenaDegraus` | opacidade `0.15 + 0.5 * suave(0.45 + k*0.023, 0.5 + k*0.023, p)` |
| marcador no índice j | aparece quando `suave(0.45, 0.75, p) * 12 >= j` (opacidade 0/1 com `transition` CSS de 200 ms) |
| `.cena-satelite` | entram entre 0,78 e 0,95 (opacidade, `translate3d(±40px→0, 0, 0)`) |
| `.cena-camera` | `scale(misturar(1, 0.88, suave(0.78, 1, p)))` |
| luz do fundo | opacidade `0.35 * suave(0.7, 1, p)` |

**Layout:**
- Desktop: cartão de 440px centrado em ~68% da largura da cena, à direita do texto.
- Retrato/celular (`(max-width: 767px), (max-aspect-ratio: 9/10)`, a mesma regra que já existe): cartão com 86vw de largura máxima no terço superior; o texto continua embaixo.

- [ ] **Passo 1: Escrever os testes que falham.**
  - `LiquidGlassMarkupTest.aberturaTemCartaoDemonstrativo`:
    - `index.html` contém `id="cenaCarteira"`, `Dados demonstrativos`, `data-inclina="6"` e `data-inclina-area="#jornada"`;
    - não contém `id="cenaPessoa"` nem `id="cenaCidade"`;
    - tem exatamente uma ocorrência de `<h1`.
  - Em `landing_regression.py`:
    - `cena_acompanha_scroll(page)`, viewport 1280×720:
      - p = 0,02 → `#cenaValor` = `"R$ 100.000,00"` e o primeiro chip com opacidade < 0,1;
      - p = 0,62 → `#cenaLinha` com `stroke-dashoffset` < 0,7 e os 4 chips com opacidade > 0,9;
      - p = 0,98 → `#cenaValor` = `"R$ 112.400,00"` e `.cena-satelite` visíveis;
      - p = 0,02 de novo → volta ao primeiro estado.
    - `scroll_brusco_mantem_cena_coerente(page)`: `scrollTo` direto do topo para o fim e de volta, esperando 1,5 s → mesmos asserts de p = 0,98 e p = 0,02.
    - `redimensionar_sem_overflow(page)`: em p = 0,6, `set_viewport_size(390, 844)`, esperar 0,5 s → `document.documentElement.scrollWidth <= innerWidth` e a caixa de `#cenaCarteira` inteira dentro da viewport.
    - `ponteiro_inclina_cartao(page)`: mover o mouse para (1200, 200) dentro de `#jornada` → `#cenaCarteira.style.transform` contém `rotateY(`.
    - `teclado_foca_botoes_visiveis(page)`: Tab até "Começar agora" → o botão está dentro da viewport e com opacidade > 0,9.
    - `movimento_reduzido_mostra_estado_final(browser)`: `#cenaValor` = `"R$ 112.400,00"`, sem `transform` inline no cartão.
    - `screenshots(browser)`: 1280×720, 768×1024 e 390×844 em p = 0,02, 0,35, 0,62 e 0,98, salvas em `target/landing-screens/`.

- [ ] **Passo 2: Rodar e ver falhar** (comandos da Tarefa 2, passo 3) → falham `aberturaTemCartaoDemonstrativo` e `cena_acompanha_scroll`.

- [ ] **Passo 3: Implementar** markup, `cena` nova (`{ atualizar(p), enquadrar() }`, a mesma interface que o laço já chama) e CSS.

- [ ] **Passo 4: Rodar e ver passar**, e abrir as screenshots de `target/landing-screens/` para checar a composição nos 3 tamanhos (cartão inteiro, nada sobre o texto, sem neon).

- [ ] **Passo 5: Commit.**
  ```bash
  git add src/main/resources/templates/index.html src/main/resources/static/js/landing.js src/main/resources/static/css/landing.css src/test
  git commit -m "feat(landing): abertura com carteira demonstrativa que ganha forma no scroll"
  ```

---

### Task 4: Cabeçalho vivo (link da seção ativa + luz no CTA) + Open Graph

**Arquivos:**
- Modificar: `src/main/resources/static/js/landing.js` (pequeno bloco "cabecalho" no fim do IIFE, antes do `if (reduzMovimento)`, para valer também com movimento reduzido)
- Modificar: `src/main/resources/static/css/landing.css`
- Modificar: `src/main/resources/templates/index.html`:
  - o "Criar conta" do header ganha `data-luz`;
  - no `<head>`, `og:title`, `og:description` e `og:type` (`website`), com o mesmo texto do `<title>` e da meta description.
- Teste: `src/test/playwright/landing_regression.py`, `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`

**Interfaces:**
- Consome: `data-luz` (Tarefa 2).

- [ ] **Passo 1: Escrever os testes que falham.**
  - `LiquidGlassMarkupTest.landingTemOpenGraph`: `index.html` contém `property="og:title"`, `property="og:description"` e `property="og:type" content="website"`.
  - `cabecalho_marca_secao_ativa(page)`: rolar até `#como-funciona` → o link `a[href="#como-funciona"]` do header tem `aria-current="true"` e nenhum outro tem.

- [ ] **Passo 2: Rodar e ver falhar.**

- [ ] **Passo 3: Implementar.**
  - IntersectionObserver sobre as seções cujos ids aparecem nos links do header, com `rootMargin: '-45% 0px -50% 0px'`. Liga `aria-current="true"` no link da seção ativa.
  - CSS: sublinhado de 2px que cresce com `transform: scaleX` em `[aria-current="true"]`.

- [ ] **Passo 4: Rodar e ver passar.**

- [ ] **Passo 5: Commit.** `git commit -m "feat(landing): cabecalho marca a secao ativa e CTA com luz"`

---

### Task 5: Verificação final, arquivamento e PR

- [ ] **Passo 1:** `./mvnw test` → `Tests run: N, Failures: 0`, `BUILD SUCCESS`.
- [ ] **Passo 2:** Rodar `landing_regression.py` inteiro contra a 8081 → todas as funções passam e não há erro de console nem de CSP.
- [ ] **Passo 3: Desempenho.**
  - Com Playwright e `page.context.tracing` (ou CDP `Performance`): rolar a abertura de 0 a 1 em 3 s.
  - Nenhuma tarefa > 50 ms repetida, e CLS < 0,1 (`PerformanceObserver` de `layout-shift` somando `value`).
  - Registrar os números na task da change.
- [ ] **Passo 4: Segurança.** `bash scripts/security_gate.sh` → `APROVADO`; `bash scripts/dast_zap.sh` → `APROVADO`.
- [ ] **Passo 5: Fechar.**
  - Marcar as tasks da change.
  - `openspec archive landing-abertura-carteira -y`, commit `docs(openspec): arquiva landing-abertura-carteira` com os endpoints alterados (`GET /videos/** removido`).
  - `graphify update .`
- [ ] **Passo 6:** push da `27-develop` e PR para a `main`, **só quando o usuário pedir**.
