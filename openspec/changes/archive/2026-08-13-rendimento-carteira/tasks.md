## 1. Backend computation

- [x] 1.1 Create `dto/RendimentoPontoDTO.java` — record `(LocalDateTime timestamp, BigDecimal rendimento)`
- [x] 1.2 Create `service/RendimentoService.java` — `calcularSerie(Long carteiraId)`: gather ATIVA operações (`OperacaoRepository.findByCarteiraIdAndStatusOrderByDataHoraAsc`) and, for each distinct ação among them, its `HistoricoCotacao` rows; build sorted distinct sample timestamps (union of operação `dataHora` + `HistoricoCotacao.capturadoEm`); for each sample, filter operações `<= T`, group by `(acao, corretora)`, run `PosicaoCalculator.calcular` per group, resolve each held ação's latest known price `<= T`, compute `naoRealizado` + `realizado` per design.md's formulas, return ordered `List<RendimentoPontoDTO>`
- [x] 1.3 Add `buscarRendimentoPropria(String emailUsuarioAutenticado)` in `CarteiraService`, mirroring `buscarSaldoPropria`'s usuario/carteira lookup, delegating the calculation to `RendimentoService`
- [x] 1.4 Add `GET /carteiras/me/rendimento-historico` in `CarteiraController`

## 2. Frontend chart

- [x] 2.1 Add a new panel in `dashboard.html`'s `#view-visao-geral`, between "Resultado da carteira" and "Minhas posições" — panel head "Rendimento" + an `<svg id="rendimentoChart">` container with an empty-state fallback
- [x] 2.2 In `dashboard.js`, add `api("/carteiras/me/rendimento-historico")` to the `Promise.all` in `carregarTudo()`, store as `state.rendimento`
- [x] 2.3 In `dashboard.js`, add a `renderRendimentoChart()` function: hand-drawn SVG polyline (viewBox-based, scale points to fit, color positive/negative via `--rendo-color-accent`/`--rendo-color-danger` matching `.pl-pos`/`.pl-neg`), called from `renderVisaoGeral()`; empty-state message when the series has 0-1 points (not enough to draw a line)
- [x] 2.4 Add matching styles in `dashboard.css` for the new panel/svg container (reuse `.panel`/`.result-hint` conventions, no new design language)

## 3. Verification

- [x] 3.1 Run the existing test suite — no behavior change to existing endpoints
- [x] 3.2 Manually verify in a real browser: with 0 operações, chart shows empty state; after a compra + a cotação update + a venda, the chart shows a multi-point line and the shape matches the expected direction (up after a gain, down after a loss)
