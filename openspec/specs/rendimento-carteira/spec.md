# rendimento-carteira Specification

## Purpose

Gives the user a view of how their portfolio's total return evolved over time, reconstructed from existing operation and quote-history records rather than a separately maintained snapshot.

## Requirements

### Requirement: Portfolio return series is computed from history
The system SHALL compute, for a given carteira, a time-ordered series of total return (realized + unrealized combined) sampled at each point where either a trade occurred or a quote was captured for a stock the carteira has held.

#### Scenario: Carteira with trades and quote updates
- **WHEN** a carteira has one or more ATIVA operações and one or more `HistoricoCotacao` points for the ações it has traded
- **THEN** the system returns one series point per distinct timestamp in the union of those operação and quote-history timestamps, ordered chronologically

#### Scenario: Carteira with no operations yet
- **WHEN** a carteira has no ATIVA operações
- **THEN** the system returns an empty series

### Requirement: Each series point reflects state as of that time only
Each point's return value SHALL be computed using only operações with `dataHora` at or before that point's timestamp, and using each stock's most recent known quote at or before that point's timestamp.

#### Scenario: Price unknown yet at an early point
- **WHEN** a sample timestamp is earlier than the first known `HistoricoCotacao` point for a stock the carteira already held at that time
- **THEN** that stock is excluded from the market-value portion of that point's calculation (no fabricated price)

### Requirement: Return combines realized and unrealized
Each point's value SHALL equal the sum of realized gains from VENDA operações up to that point (using each sale's already-stored `precoMedioNaVenda`) plus the unrealized gain of positions still held at that point (market value at the point's prices minus their cost basis).

#### Scenario: Point after a sale
- **WHEN** a sample point falls after a VENDA operação
- **THEN** that sale's realized profit/loss is included in the point's total, and it remains included in every later point's total

## Non-Requirements

- No scheduled/background pre-computation — the series is computed on read from existing stored data (`Operacao`, `HistoricoCotacao`, `Carteira.saldoInicial`), matching this project's existing "compute on read where cheap" pattern used for the current-balance calculation.
