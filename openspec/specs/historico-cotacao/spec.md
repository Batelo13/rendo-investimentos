# historico-cotacao Specification

## Purpose

Keeps a real, queryable time series of every quote fetched for a stock, so features that need price evolution over time (like a performance chart) have actual data instead of only the latest snapshot.

## Requirements

### Requirement: Quote fetch is recorded as a history point
Whenever the system successfully fetches a quote for an `Acao` (on cadastro or on an explicit "atualizar cotação" request), it SHALL persist a `HistoricoCotacao` record capturing the price and the capture time, in addition to updating `Acao.cotacaoAtual`.

#### Scenario: Ação cadastrada
- **WHEN** a new `Acao` is registered and its initial quote is fetched successfully
- **THEN** a `HistoricoCotacao` record is created for that `Acao` with the fetched price and timestamp

#### Scenario: Cotação atualizada
- **WHEN** a user triggers "atualizar cotação" for an existing `Acao` and the fetch succeeds
- **THEN** a new `HistoricoCotacao` record is created (in addition to the existing one), leaving prior records untouched

#### Scenario: Fetch fails
- **WHEN** the quote fetch fails (external provider error)
- **THEN** no `HistoricoCotacao` record is created, matching the existing behavior where `Acao.cotacaoAtual` is also left unchanged

### Requirement: History records are immutable
Once created, a `HistoricoCotacao` record SHALL NOT be edited or deleted through normal application flow.

#### Scenario: Repeated fetches accumulate
- **WHEN** a quote is fetched multiple times for the same `Acao` over time
- **THEN** each fetch adds a new record and none of the previous records are modified or removed

### Requirement: Quote history is readable per ação
The system SHALL expose the stored quote history for a given `Acao`, ordered from most recent to oldest.

#### Scenario: Reading history
- **WHEN** a client requests the quote history for an `Acao` by id
- **THEN** the system returns all `HistoricoCotacao` records for that `Acao`, ordered by capture time descending
