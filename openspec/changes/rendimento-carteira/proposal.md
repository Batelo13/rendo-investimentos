## Why

The user wants a performance chart on the dashboard showing how the whole portfolio's return evolved over time, not just its current snapshot (the existing "Lucro/Prejuízo" stat cards only show *now*). This is now unblocked by [[historico-cotacao]], which gives real historical prices per stock.

## What Changes

- New read-only computation that reconstructs total portfolio return (realized + unrealized) at a series of past points in time, by replaying operation history and matching each point to the best-known historical price.
- New endpoint `GET /carteiras/me/rendimento-historico` returning the ordered series.
- New panel in the dashboard's "Visão geral" view with a hand-drawn SVG line chart (no new dependency) plotting the series.

## Capabilities

### New Capabilities
- `rendimento-carteira`: a time series of the user's total portfolio return (realized + unrealized combined), computed on read from existing operation and quote-history data.

### Modified Capabilities
(none — purely additive read path; no existing entity, endpoint, or stored value changes)

## Impact

- New service logic reusing `PosicaoCalculator.calcular(List<Operacao>)` and `SaldoCalculator.calcular(saldoInicial, List<Operacao>)` — both already pure functions over an arbitrary ordered `Operacao` list, so no changes needed to either.
- New endpoint on `CarteiraController` (or wherever `/carteiras/me/...` routes live).
- `dashboard.html`/`dashboard.js`: new chart panel + rendering code in the Visão Geral view.
- No new dependency (SVG hand-drawn, per explicit decision — a charting library was considered and declined).
- Data density is bounded by the same on-demand-only limitation as `historico-cotacao` — documented there, applies here too since this series samples at the union of trade times and quote-capture times.
