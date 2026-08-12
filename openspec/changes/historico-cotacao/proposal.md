## Why

`Acao.cotacaoAtual` is overwritten in place every time a quote is fetched — there is no record of what the price was before. This blocks a "gráfico de rendimento" (yield/performance chart) the user wants on the dashboard, which needs a real time series to plot, not just the latest snapshot. This was already flagged as a known gap in the project's roadmap.

## What Changes

- New `HistoricoCotacao` entity: an append-only row per quote fetch (never edited or deleted), mirroring the immutable-history pattern already used by `Operacao`.
- `AcaoService.atualizarCotacao()` and `criar()` — the two existing call sites that already fetch a quote via `CotacaoProvider` — additionally persist a `HistoricoCotacao` row each time, using the same `DadosCotacaoResponse` they already have in hand.
- New read endpoint `GET /acoes/{id}/historico` returning the stored series, ordered by capture time.
- Capture stays **on-demand only** (same trigger as today: the "atualizar cotação" action and initial cadastro) — no new scheduled job. This is a deliberate scope decision: a `@Scheduled` background capture was considered and explicitly deferred, not forgotten. Consequence: the series will only have points where a user actually triggered a refresh, so it may be sparse for lightly-used stocks. Documented here so it isn't mistaken for a bug later.

## Capabilities

### New Capabilities
- `historico-cotacao`: append-only time series of stock quotes, captured whenever a quote is fetched, readable via a dedicated endpoint.

### Modified Capabilities
(none — `Acao.cotacaoAtual` behavior is unchanged, this only adds a side-effect write and a new read path)

## Impact

- New table `historico_cotacoes` (JPA `ddl-auto`, no manual migration in this project).
- `AcaoService`: two existing methods gain one additional persistence call each; no change to their existing return values or error handling.
- `AcaoController`: one new `GET` endpoint.
- No changes to `CotacaoProvider`/`BrapiCotacaoProvider`/`TwelveDataCotacaoProvider` — `DadosCotacaoResponse` already carries everything needed.
- Prerequisite for a separate, later change: the dashboard "gráfico de rendimento" (not part of this change).
