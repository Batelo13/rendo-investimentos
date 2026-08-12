## Context

See proposal.md - Why. `PosicaoCalculator.calcular(List<Operacao>)` and `SaldoCalculator.calcular(saldoInicial, List<Operacao>)` are pure static folds with no "today" assumption (confirmed by reading both) — they compute correctly for any chronologically-ordered subset of operações, which is exactly what "position as of time T" needs. `HistoricoCotacao` (see [[historico-cotacao]]) now provides historical prices per ação. No entity in this codebase stores a "portfolio value over time" snapshot — this change computes the series on read, not on write.

## Goals / Non-Goals

**Goals:**
- Reuse `PosicaoCalculator`/`SaldoCalculator` unchanged.
- Produce a chronological series usable directly by a line chart.

**Non-Goals:**
- Any new stored/cached time-series table for portfolio value — out of scope, revisit only if the on-read computation proves too slow in practice (unlikely at this project's data volume).
- Sub-day/continuous interpolation between sample points — the chart connects the discrete points it has.
- A charting library — declined per explicit decision, hand-drawn SVG instead.

## Decisions

**Sampling: union of trade and quote timestamps.** Alternative considered: fixed intervals (e.g., daily buckets) — rejected because with on-demand-only quote capture, most days would have no new information, and interpolating a price between two real captures would show a return number that never actually existed. Sampling only at real events keeps every plotted point backed by real data.

**Per-point computation, reusing existing pure functions:**
1. Gather the carteira's ATIVA operações (chronological) and, for every distinct ação among them, its `HistoricoCotacao` rows (chronological).
2. Sample timestamps = sorted distinct union of all operação `dataHora` and all gathered `HistoricoCotacao.capturadoEm`.
3. For each sample timestamp `T`:
   - Filter operações to `dataHora <= T`.
   - Group by `(acao, corretora)`, run `PosicaoCalculator.calcular` per group → quantity + avg cost held at `T`.
   - For each ação with quantity > 0 at `T`, find its latest `HistoricoCotacao` with `capturadoEm <= T` → market price at `T`. If none exists yet, exclude that ação's market value from this point (see spec scenario).
   - `naoRealizado(T) = Σ(quantidade * precoMercado) - Σ(quantidade * precoMedio)` over held positions with a known price.
   - `realizado(T) = Σ (precoUnitario - precoMedioNaVenda) * quantidade` over VENDA operações with `dataHora <= T` (same formula `OperacaoService` already uses per-sale).
   - `rendimento(T) = realizado(T) + naoRealizado(T)`.
4. Return the ordered list of `{timestamp, rendimento}`.

**Where the logic lives:** a new stateless service (e.g. `RendimentoService`), not folded into `CarteiraService`, since it's a distinct read-model computation over multiple repositories (`OperacaoRepository`, `HistoricoCotacaoRepository`) rather than a simple CRUD-adjacent method — mirrors how `PosicaoCalculator`/`SaldoCalculator` already live as separate single-purpose classes rather than being crammed into `CarteiraService`.

**Complexity note:** for each sample point, computing positions requires re-filtering and re-folding the operação list, giving roughly O(samples × operações) — for this project's realistic data volume (a course project, not production trading volume) this is negligible; not worth optimizing (e.g. incremental/rolling computation) until it's an actual measured problem.

## Risks / Trade-offs

- **Sparse/uneven series** (inherits `historico-cotacao`'s on-demand-only limitation) → mitigation: already documented there; same upgrade path (a scheduled capture job) would densify this too.
- **O(samples × operações) recomputation on every read** → acceptable at this project's scale; mitigation if it ever matters: cache per carteira with invalidation on new operação/cotação, same pattern as `PosicaoAtual`.
