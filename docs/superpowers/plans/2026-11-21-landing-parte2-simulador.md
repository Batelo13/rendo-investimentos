# Landing parte 2: simulador demonstrativo (plano de implementação)

> **Para agentes:** SUB-SKILL OBRIGATÓRIA: use superpowers:subagent-driven-development (recomendado) ou superpowers:executing-plans para executar este plano tarefa por tarefa. Os passos usam checkbox (`- [ ]`) para acompanhamento.

**Objetivo:** criar a seção "Experimente". Nela o visitante escolhe até 4 ações, distribui os R$ 100.000 com sliders e vê a carteira em 3 cenários ilustrativos (anel de alocação, gráfico de 12 meses e contador do patrimônio), tudo no navegador e rotulado como demonstrativo.

**Arquitetura:** a seção nova no `index.html` traz só os contêineres e os textos fixos (rótulos, título e `<noscript>`). Um arquivo novo, `landing-simulador.js` (IIFE, sem dependências), monta os controles a partir de uma tabela de ações demo, guarda o estado e redesenha anel, gráfico e resumo. O gráfico e o anel são SVG, a interpolação roda em `requestAnimationFrame`, e a formatação usa `window.RendoMotion.formatar` da parte 1.

**Stack:** Thymeleaf, JS puro, CSS e SVG. Testes: JUnit 5 (`LiquidGlassMarkupTest`) e Playwright Python (`src/test/playwright/landing_regression.py`).

**Spec:** `docs/superpowers/specs/2026-11-20-landing-experiencia-imersiva-design.md` (seção 3 "Experimente", "Restrições", "Sistema de movimento", "Verificação" e "Entrega/parte 2").

## Restrições globais

- Nada vai ao servidor: nenhuma requisição, nada salvo, nada levado para o cadastro (decisão 5 do design).
- CSP `script-src 'self'`: nada de `<script>` inline nem `on*=`. Nenhuma dependência nova.
- Rótulos visíveis e exatos: **"Dados demonstrativos"** (selo da seção) e **"Série ilustrativa: não é histórico real nem previsão."** (sob o gráfico).
- Cotação demo fixa e rotulada: **US$ 1 = R$ 5,40 (cotação demonstrativa)**.
- Sliders nativos (`<input type="range">`, 0 a 100.000, passo 1.000) com `<label>` associado. Os cenários são `<input type="radio" name="cenario">` nativos.
- Animar só `transform`/`opacity` no CSS. A geometria SVG é recalculada no máximo uma vez por quadro (rAF).
- `prefers-reduced-motion: reduce`: tudo funciona, só que sem transição (o valor e o gráfico mudam na hora).
- Copy sem promessa de retorno; cenários se chamam **Alta**, **Lateral** e **Queda**.
- Commits pequenos em português, na `27-develop`, com `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Depois de alterar código: `graphify update .`.

## Dados demonstrativos (fonte única: topo de `landing-simulador.js`)

```js
const ACOES = [ // [ticker, nome, mercado, preco demo, beta]
  ['PETR4', 'Petrobras PN', 'BR', 38.50, 1.3],
  ['VALE3', 'Vale ON', 'BR', 61.20, 1.2],
  ['ITUB4', 'Itaú Unibanco PN', 'BR', 33.80, 0.9],
  ['WEGE3', 'WEG ON', 'BR', 41.70, 0.8],
  ['AAPL', 'Apple', 'EUA', 228.00, 1.1],
  ['MSFT', 'Microsoft', 'EUA', 415.00, 1.0],
];
const DOLAR = 5.40;
const MERCADO = { // indice ilustrativo do mercado, 13 pontos (mes 0..12)
  alta:    [1, 1.012, 1.004, 1.026, 1.041, 1.035, 1.058, 1.072, 1.066, 1.089, 1.103, 1.097, 1.124],
  lateral: [1, 1.008, 0.994, 1.006, 1.012, 0.998, 1.004, 1.015, 1.001, 0.992, 1.006, 1.011, 1.003],
  queda:   [1, 0.991, 0.978, 0.986, 0.962, 0.955, 0.941, 0.948, 0.929, 0.918, 0.927, 0.909, 0.896],
};
const INICIAL = { PETR4: 40000, VALE3: 25000, WEGE3: 20000 }; // caixa 15.000; cenario inicial: alta
```

**Modelo** (`serie(estado) -> number[13]`):
- Para a ação `k` (índice em `ACOES`) no mês `t`: `fator = 1 + beta * (MERCADO[cenario][t] - 1) + 0.006 * Math.sin(t * (1.1 + 0.3 * k))`. Em `t = 0`, `fator = 1`.
- `valor(t) = caixa + Σ alocado_i * fator_i(t)`, sendo `caixa = 100000 - Σ alocado_i`.
- O modelo é determinístico: a mesma entrada sempre gera a mesma série.

## Foco de revisão

1. **Arrastar um slider rápido** (dezenas de eventos `input`): a página não trava e o gráfico acompanha. → `simulador_arrastar_sem_travar` (Task 3).
2. **Teclado no slider** (setas, PageUp, End): o limite de R$ 100.000 continua valendo. → `simulador_teclado_respeita_limite` (Task 2).
3. **Marcar 4 ações e desmarcar uma**: as outras voltam a ficar disponíveis e o valor desmarcado volta ao caixa. → `simulador_escolhe_ate_4_acoes` e `simulador_desmarcar_devolve_ao_caixa` (Task 2).
4. **Celular de 390 px**: nada de scroll horizontal, e sliders e cenários ficam utilizáveis. → `simulador_sem_overflow_no_celular` (Task 3).
5. **Trocar de cenário no meio da animação** (Alta → Queda rápido): termina exatamente no valor de Queda. → `simulador_troca_rapida_de_cenario` (Task 3).

---

### Task 1: Change OpenSpec + seção "Experimente" + link no cabeçalho

**Arquivos:**
- Criar: `openspec/changes/landing-simulador-demonstrativo/{proposal.md,design.md,tasks.md,specs/pagina-inicial-imersiva/spec.md}`
- Modificar: `src/main/resources/templates/index.html`:
  - nova `<section class="lp-secao lp-simulador" id="experimente" data-motion>` logo depois de `#visao-geral`;
  - link `<a href="#experimente">Experimente</a>` entre "A Rendo" e "Como funciona" no `.lp-nav`;
  - `<script th:src="@{/js/landing-simulador.js}"></script>` depois do `landing.js`.
- Criar: `src/main/resources/static/js/landing-simulador.js` (por enquanto só o IIFE que sai cedo se não achar `#experimente`)
- Teste: `src/test/java/com/curso/gestaoinvestimentos/ui/LiquidGlassMarkupTest.java`

**Interfaces:**
- Produz (IDs que as Tasks 2 e 3 preenchem):
  - `#simAcoes` (`div`, chips);
  - `#simSliders` (`div`, sliders);
  - `#simCaixa` (`output`, texto do caixa);
  - `#simCenarios` (`fieldset`, com os 3 radios `value="alta|lateral|queda"`, `alta` marcado no HTML);
  - `#simAnel` (`svg`, viewBox `0 0 120 120`);
  - `#simGrafico` (`svg`, viewBox `0 0 560 220`);
  - `#simValor` (`strong`);
  - `#simResumo` (`p`, `aria-live="polite"`);
  - `#simAviso` (`p`, "Escolha até 4 ações.");
  - o CTA `a.lp-botao-primario` → `/login?criarConta=1`.
- Textos fixos:
  - título "Monte uma carteira de demonstração";
  - selo "Dados demonstrativos";
  - legenda "Série ilustrativa: não é histórico real nem previsão.";
  - "US$ 1 = R$ 5,40 (cotação demonstrativa)";
  - `<noscript><p>Ative o JavaScript para experimentar o simulador.</p></noscript>`;
  - CTA "Criar conta e montar a carteira de verdade".

- [ ] **Passo 1:** criar a change OpenSpec.
  - **proposal:** sem endpoint novo e sem banco.
  - **design:**
    - decisões: modelo por beta × índice de mercado; cenários em vez de retorno único; nada sai do navegador;
    - STRIDE: sem entrada no servidor, nada persistido, nenhum dado pessoal.
  - **delta ADDED em `pagina-inicial-imersiva`:**
    - requirement "Simulador demonstrativo", com os cenários das Tasks 2 e 3;
    - requirement "Rótulos demonstrativos no simulador", com os textos exatos acima.
  - **tasks** citando os testes.
  - Validar com `openspec validate landing-simulador-demonstrativo --strict` → `is valid`.
- [ ] **Passo 2: teste que falha** `LiquidGlassMarkupTest.landingTemSimuladorDemonstrativo`. O `index.html` contém:
  - `id="experimente"`, `href="#experimente"`, `Dados demonstrativos`;
  - `Série ilustrativa: não é histórico real nem previsão.`;
  - `US$ 1 = R$ 5,40 (cotação demonstrativa)`;
  - `aria-live="polite"`, `<noscript>`;
  - `@{/js/landing-simulador.js}` depois de `@{/js/landing.js}` (comparar `indexOf`);
  - `name="cenario"` 3 vezes.
- [ ] **Passo 3:** `./mvnw test -Dtest=LiquidGlassMarkupTest` → FAIL em `landingTemSimuladorDemonstrativo`.
- [ ] **Passo 4:** markup da seção, link, script e o JS mínimo.
  - **Layout:** grade de 2 colunas em ≥ 1000 px, com controles à esquerda e resultado à direita.
  - O painel de resultado é um `div.sim-resultado.glass-surface-strong` com `data-luz`.
- [ ] **Passo 5:** mesmo comando → `BUILD SUCCESS`.
- [ ] **Passo 6: commit** `feat(landing): secao Experimente do simulador demonstrativo (estrutura)`.

---

### Task 2: Escolha de ações, sliders, caixa e conversão

**Arquivos:**
- Modificar: `src/main/resources/static/js/landing-simulador.js`
- Modificar: `src/main/resources/static/css/landing.css` (bloco "Simulador")
- Teste: `src/test/playwright/landing_regression.py`

**Interfaces:**
- Consome: `window.RendoMotion.formatar(valor, 'brl0')` (parte 1) e os IDs da Task 1.
- Produz:
  - estado interno `{ alocado: Map<ticker, number>, cenario: 'alta'|'lateral'|'queda' }`;
  - `redesenhar()`, chamado depois de qualquer mudança (a Task 3 completa o corpo);
  - DOM estável para testes:
    - chips `#simAcoes button[data-ticker]` com `aria-pressed`;
    - slider `#simSliders input[type=range][data-ticker]`;
    - valor do slider `output[data-valor-de="<ticker>"]`.

**Regras:**
- **Chips:** um `button` por ação, mostrando ticker, nome e mercado.
  - Clicar alterna a seleção.
  - Com 4 selecionados, os não selecionados ficam `disabled` e `#simAviso` aparece.
  - Desmarcar remove o slider e devolve o valor ao caixa.
- **Slider:** em cada `input`, o valor vira `min(valor, 100000 - soma dos outros)`, e o `input.value` é reescrito com o valor limitado.
- **Rótulo do slider:** `R$ 40.000`. Para ações dos EUA, mostra também `≈ US$ 7.407`, com `Intl.NumberFormat('pt-BR', {style:'currency', currency:'USD', minimumFractionDigits:0, maximumFractionDigits:0})` sobre `valor / 5.40`.
- **`#simCaixa`:** `Caixa: R$ 15.000`.

- [ ] **Passo 1: testes que falham** em `landing_regression.py`. O helper `slider(page, ticker, valor)` define `value` e dispara `input`; o helper `ir_para_simulador(page)` rola até `#experimente`.
  - `simulador_estado_inicial`:
    - chips PETR4, VALE3 e WEGE3 com `aria-pressed="true"`;
    - `#simCaixa` = `"Caixa: R$ 15.000"`.
  - `simulador_limita_soma_em_100_mil`:
    - `slider(PETR4, 100000)` → o valor do PETR4 fica 55.000 (100.000 − 25.000 − 20.000);
    - `#simCaixa` = `"Caixa: R$ 0"`.
  - `simulador_teclado_respeita_limite`:
    - focar o slider VALE3 e apertar `End` → VALE3 = 100.000 − PETR4 − WEGE3;
    - `#simCaixa` = `"Caixa: R$ 0"`.
  - `simulador_escolhe_ate_4_acoes`:
    - clicar em AAPL → 4 selecionados; ITUB4 e MSFT `disabled`; `#simAviso` visível;
    - clicar em AAPL de novo → ITUB4 e MSFT habilitados; `#simAviso` oculto.
  - `simulador_desmarcar_devolve_ao_caixa`:
    - desmarcar VALE3 → o slider some e `#simCaixa` = `"Caixa: R$ 40.000"`.
  - `simulador_converte_acoes_eua`:
    - marcar AAPL e `slider(AAPL, 10000)` → o rótulo contém `"R$ 10.000"` e `"US$ 1.852"`.
- [ ] **Passo 2:** subir a instância descartável e rodar `RENDO_URL=http://localhost:8081 python src/test/playwright/landing_regression.py` → `AssertionError`/timeout em `simulador_estado_inicial`.
  - Comando da instância: `./mvnw -q -B package -DskipTests && java -jar target/gestao-investimentos-0.0.1-SNAPSHOT.jar --server.port=8081 "--spring.datasource.url=jdbc:h2:mem:landing;DB_CLOSE_DELAY=-1" --spring.jpa.hibernate.ddl-auto=create-drop --app.mail.enabled=false --app.admin.emails= &`
- [ ] **Passo 3:** implementar as regras acima e o CSS:
  - chips em `flex-wrap`;
  - trilho do slider com `accent-color: var(--lp-esmeralda-claro)`;
  - alvo de toque ≥ 44 px de altura.
- [ ] **Passo 4:** rodar de novo → as 6 funções passam, junto com as 15 da parte 1.
- [ ] **Passo 5: commit** `feat(landing): simulador escolhe acoes e distribui o saldo com limite de R$ 100.000`.

---

### Task 3: Cenários, gráfico, anel, contador e resumo acessível

**Arquivos:**
- Modificar: `src/main/resources/static/js/landing-simulador.js`, `src/main/resources/static/css/landing.css`
- Teste: `src/test/playwright/landing_regression.py`

**Interfaces:**
- Consome:
  - estado e `redesenhar()` (Task 2);
  - `ACOES`, `MERCADO` e o modelo `serie(estado)` (cabeçalho deste plano);
  - `window.RendoMotion.formatar(v, 'brl')` e `window.RendoMotion.reduzMovimento`.

**Regras:**
- **Gráfico** (`#simGrafico`, 560×220): `polyline.sim-linha` mais uma área (`path.sim-area`, preenchimento esmeralda com opacidade 0,12).
  - Eixo Y fixo de 85.000 a 120.000, para os cenários serem comparáveis entre si.
  - Rótulos "mês 0" e "mês 12".
  - Linha de referência tracejada em R$ 100.000.
- **Troca de estado:** os pontos interpolam do desenho atual até o novo em 500 ms (easeOutCubic, rAF). Uma troca no meio da animação parte do desenho atual. Com `reduzMovimento`, troca na hora.
- **Anel** (`#simAnel`): uma fatia por ação selecionada, com a cor do chip, mais uma fatia de caixa `rgba(255,255,255,0.12)`. Mesma técnica `pathLength="1"` + `stroke-dasharray`/`dashoffset` da abertura.
  - Paleta: `['#5DE2BA', '#2E9E7B', '#D4B07A', '#8FA79F']`, na ordem de seleção.
- **`#simValor`:** valor do mês 12 com `formatar(v, 'brl')`. Interpola junto com o gráfico e termina sempre no valor exato.
- **`#simResumo`:** `"Cenário de <alta|lateral|queda>: R$ <valor> após 12 meses, com <n> ações e R$ <caixa> em caixa."` (ilustrativo). Atualizado ~400 ms depois da última mudança (debounce), para o leitor de tela não ser inundado durante o arraste.

- [ ] **Passo 1: testes que falham.**
  - `simulador_tudo_em_caixa_nao_varia`: desmarcar as 3 ações e, para cada cenário, esperar 0,7 s → `#simValor` = `"R$ 100.000,00"`.
  - `simulador_cenarios_ordenados`: no estado inicial, ler `#simValor` em alta, lateral e queda (0,7 s cada, convertendo para número) → alta > lateral > queda, e queda < 100.000 < alta.
  - `simulador_deterministico`: alta → queda → alta → o valor final é igual ao primeiro valor de alta.
  - `simulador_troca_rapida_de_cenario`: clicar em queda e, 100 ms depois, em lateral, esperar 0,8 s → valor igual ao de lateral medido isoladamente.
  - `simulador_resumo_aria_live`: depois de clicar em queda e esperar 0,8 s, `#simResumo` começa com `"Cenário de queda: R$"`.
  - `simulador_movimento_reduzido`: no contexto `reduced_motion="reduce"`, clicar em queda → 50 ms depois o `#simValor` já é o valor final de queda (igual ao lido após 1 s).
  - `simulador_arrastar_sem_travar`:
    - `PerformanceObserver('longtask')`;
    - arrastar o slider PETR4 com o mouse de ponta a ponta em 30 passos → no máximo 1 tarefa longa;
    - `#simValor` coerente com o slider final (valor diferente do inicial).
  - `simulador_sem_overflow_no_celular`: viewport 390×844, ir ao simulador → `scrollWidth <= innerWidth`, e todo slider e radio com caixa dentro da largura.
  - Estender `screenshots`: capturar `#experimente` em 1280, 768 e 390 (arquivo `simulador-<largura>.png`).
- [ ] **Passo 2:** rodar → falha em `simulador_tudo_em_caixa_nao_varia` (o valor não existe ou não muda).
- [ ] **Passo 3:** implementar as regras acima e o CSS (gráfico responsivo `width: 100%`, legenda em `--lp-texto-3`).
- [ ] **Passo 4:** rodar → todas passam. Abrir `target/landing-screens/simulador-*.png` e checar a composição: rótulos legíveis, sem neon, controles alinhados.
- [ ] **Passo 5: commit** `feat(landing): cenarios ilustrativos, grafico e resumo acessivel no simulador`.

---

### Task 4: Verificação final e arquivamento

- [ ] **Passo 1:** `./mvnw test` → `Failures: 0`, `BUILD SUCCESS`.
- [ ] **Passo 2:** `landing_regression.py` inteiro na 8081 → todas passam, sem erro de console nem de CSP. `desempenho_scroll` continua com CLS < 0,1.
- [ ] **Passo 3:** `bash scripts/security_gate.sh` → `APROVADO`; `bash scripts/dast_zap.sh` → `APROVADO`.
- [ ] **Passo 4:**
  - marcar as tasks da change;
  - `openspec archive landing-simulador-demonstrativo -y`;
  - commit `docs(openspec): arquiva landing-simulador-demonstrativo` com "Endpoints alterados: nenhum";
  - `graphify update .`.
- [ ] **Passo 5:** sem push nem PR. O usuário escolheu um PR único no fim da parte 3.
