## Context

See proposal.md - Why. `AcaoService.atualizarCotacao()` and `criar()` are the only two places that call a `CotacaoProvider` and already have a `DadosCotacaoResponse` (ticker, nomeEmpresa, moeda, cotacaoAtual, dataHoraCotacao) in hand at the point they overwrite `Acao.cotacaoAtual`. `Operacao` is the existing immutable-history entity in this codebase (append-only, `@ManyToOne` FK, `JpaRepository` with a derived `findByXOrderByYDesc` finder) and is the pattern to mirror.

## Goals / Non-Goals

**Goals:**
- Every successful quote fetch leaves a permanent, queryable record.
- No behavior change to the existing `cotacaoAtual` overwrite or to `CotacaoProvider`/its implementations.

**Non-Goals:**
- Scheduled/background capture (`@Scheduled`) — explicitly deferred; capture stays tied to the existing on-demand triggers only.
- Downsampling, retention limits, or aggregation of history rows — not needed at this stage's data volume.
- The chart UI itself — separate, later change.

## Decisions

**Entity shape:** `HistoricoCotacao { id, acao (ManyToOne Acao, FK acao_id, nullable=false), preco (BigDecimal), capturadoEm (LocalDateTime) }`. Mirrors `Operacao`'s style (`@Entity`, `@ManyToOne` + `@JoinColumn`, `@Enumerated` not needed here since there's no enum). `preco` as `BigDecimal` per the project's money-field rule (never `double`).

**Where the write happens:** inside `AcaoService`, right after each of the two existing `acao.setCotacaoAtual(...)` calls, save a `HistoricoCotacao` row in the same method — not a separate event/listener. Alternative considered: an `@EventListener`/domain-event approach to decouple the write; rejected as unnecessary indirection for two call sites in one service (explicit anti-overengineering rule — no new abstraction without justification).

**On-demand vs scheduled capture:** kept on-demand (matches current trigger), per explicit decision with the user. Alternative (a `@Scheduled` job polling all `Acao` rows periodically) was considered and rejected for now — it would add a new concern (background job, cron config, handling external-API rate limits/failures unattended) beyond what's needed to unblock the chart. Documented as the upgrade path if the chart ends up looking too sparse in practice.

**Read endpoint shape:** `GET /acoes/{id}/historico` returns the raw ordered list (no pagination) — history volume is bounded by how often a user manually refreshes a given stock, which is low in practice for this on-demand-only design.

## Risks / Trade-offs

- **Sparse series** (see proposal) → mitigation: documented as a known, intentional limitation; upgrade path is adding a `@Scheduled` capture job later if needed.
- **Extra write per fetch** (one more INSERT alongside the existing `Acao` UPDATE) → negligible; same transaction boundary as the existing `repository.save(acao)` call, no new failure mode introduced.
