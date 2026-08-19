## 1. Modelo de dados

- [x] 1.1 Adicionar campo `taxaCambio` (`BigDecimal`, obrigatório) em `Operacao`

## 2. `SaldoCalculator` usa a taxa de câmbio (TDD)

- [x] 2.1 Teste cobrindo operação EUA com `taxaCambio` != 1 descontando/creditando o valor convertido
- [x] 2.2 `SaldoCalculator` multiplica por `taxaCambio`

## 3. `TwelveDataCambioClient` (novo)

- [x] 3.1 Cliente `GET /exchange_rate?symbol=USD/BRL` (mesma chave/`RestClient`/timeout de `TwelveDataCotacaoProvider`)
- [x] 3.2 Falha → `ServicoExternoIndisponivelException`, mesmo padrão dos outros clientes externos

## 4. `OperacaoService` grava e usa a taxa de câmbio

- [x] 4.1 Teste de integração: compra de ação EUA grava `taxaCambio` != 1 e desconta o saldo convertido
- [x] 4.2 `OperacaoService.registrar()` busca a taxa (EUA) ou usa `1` (BRASIL) antes de montar a `Operacao`
- [x] 4.3 Checagem de saldo insuficiente usa o custo já convertido na mensagem de erro

## 5. `AcaoResponseDTO.cotacaoAtualBRL` + `AcaoService`

- [x] 5.1 Campo `cotacaoAtualBRL` (nullable) em `AcaoResponseDTO`
- [x] 5.2 Teste: populado para EUA, `null` para BRASIL, `null` com degradação graciosa se a busca da taxa falhar
- [x] 5.3 `AcaoService` busca a taxa uma única vez por listagem (não por ação), loga e segue com `null` em caso de falha
- [x] 5.4 Suíte completa após a mudança — nada mais quebrou

## 6. Frontend — helpers e exibição

- [x] 6.1 Helpers `taxaCambioPorTicker`/`fmtConvertido` em `dashboard.js` + CSS `.valor-convertido`
- [x] 6.2 Catálogo de Ações — cotação atual
- [x] 6.3 Painel de detalhes de uma ação
- [x] 6.4 Visão Geral — mini-lista de posições e card "Não realizado" (soma convertendo cada posição EUA antes de somar)
- [x] 6.5 Minhas Posições — tabela
- [x] 6.6 Modal de compra/venda — resumo ("Valor total"/"Resultado estimado")

## 7. Correção de acompanhamento — histórico e realizado usam a taxa gravada na operação

Achado durante a verificação manual: histórico de operações e "Não realizado" recalculavam com a taxa atual em vez da taxa gravada em cada `Operacao`, misturando os dois princípios do design.

- [x] 7.1 `OperacaoResponseDTO` expõe `taxaCambio` da própria operação
- [x] 7.2 `OperacaoService`/`dashboard.js` usam a taxa gravada (histórico e "realizado"), não a taxa atual
- [x] 7.3 Corrige mistura de moeda no "realizado" e cobre a tabela de operações no frontend

## 8. Verificação

- [x] 8.1 Suíte completa (`./mvnw.cmd test`) — 55 testes, 0 falhas
- [x] 8.2 Verificação manual no navegador (cotação convertida no catálogo e nos detalhes, resumo de compra com valor em R$ antes de confirmar, painel de posições e "Não realizado" convertidos, saldo descontado no valor certo, sem erros novos no console)
