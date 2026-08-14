## 1. Entity and repository

- [x] 1.1 Create `model/HistoricoCotacao.java` — `@Entity @Table(name = "historico_cotacoes")`, fields `id`, `acao` (`@ManyToOne`, `@JoinColumn(name = "acao_id", nullable = false)`), `preco` (`BigDecimal`), `capturadoEm` (`LocalDateTime`)
- [x] 1.2 Create `repository/HistoricoCotacaoRepository.java` — `JpaRepository<HistoricoCotacao, Long>` with `findByAcaoIdOrderByCapturadoEmDesc(Long acaoId)`

## 2. Wire capture into AcaoService

- [x] 2.1 In `AcaoService.atualizarCotacao()`, after `acao.setCotacaoAtual(...)`/`setDataHoraCotacao(...)`, persist a `HistoricoCotacao` row from the same `DadosCotacaoResponse`
- [x] 2.2 In `AcaoService.criar()`, do the same at its initial-quote-fetch point

## 3. Read endpoint

- [x] 3.1 Add `GET /acoes/{id}/historico` in `AcaoController`, returning the ordered history (reuse a simple DTO, e.g. `HistoricoCotacaoResponseDTO(preco, capturadoEm)`)

## 4. Verification

- [x] 4.1 Manually verify: cadastrar uma ação cria 1 registro de histórico; atualizar cotação adiciona outro sem apagar o anterior; `GET /acoes/{id}/historico` retorna os pontos em ordem decrescente
- [x] 4.2 Confirm existing `AcaoService`/`AcaoController` tests still pass (no behavior change to existing endpoints)
