## Why

Ações do mercado `EUA` já mostravam `cotacaoAtual` em dólar, mas o saldo virtual da carteira é todo em BRL. `SaldoCalculator`/`OperacaoService.registrar()` tratavam `precoUnitario × quantidade` como reais direto, sem nenhuma conversão, mesmo para ações EUA — comprar 10 ações a US$310,49 descontava "R$3.104,90" do saldo em vez do equivalente real em reais, e a mensagem de saldo insuficiente mostrava esse número bruto rotulado como R$. O usuário pediu para corrigir esse bug junto com a exibição do valor convertido em reais ao lado do dólar.

## What Changes

- Novo campo `taxaCambio` (`BigDecimal`, obrigatório) em `Operacao` — `1` para ações BRASIL, taxa USD→BRL vigente no momento do registro para ações EUA, gravada de forma imutável (mesma filosofia já usada pro histórico de cotação: fonte da verdade determinística, nunca recalculada com dado "de agora").
- `SaldoCalculator` passa a multiplicar por `taxaCambio` ao descontar/creditar o saldo — comportamento de ações BRASIL fica idêntico ao de hoje (`taxaCambio = 1`).
- Nova integração `TwelveDataCambioClient` (`GET /exchange_rate?symbol=USD/BRL`, mesma chave/API já usada pra cotação de ações EUA), sem interface — só uma fonte, sem Strategy a resolver.
- `AcaoResponseDTO` ganha `cotacaoAtualBRL` (nullable): valor convertido para ações EUA, `null` para BRASIL ou se a busca da taxa falhar (degradação graciosa em leitura — não derruba a listagem).
- Frontend (`dashboard.js`): helpers `taxaCambioPorTicker`/`fmtConvertido` derivam a taxa de `acao.cotacaoAtualBRL` já carregado (sem campo novo em `PosicaoDTO`), aplicados no catálogo de Ações, Visão Geral, Minhas Posições e no resumo do modal de compra/venda.
- Correção de acompanhamento (achada na verificação manual): histórico de operações e o card "Não realizado" usam a taxa de câmbio **gravada na própria operação**, não a taxa atual — mesmo princípio do campo `taxaCambio`, aplicado também na exibição (`OperacaoResponseDTO` ganha o campo, `dashboard.js` para de recalcular com a taxa "de agora").

Fora de escopo (decisão explícita, não construído agora):
- **Gráfico de Rendimento** (`RendimentoService`): continua misturando USD/BRL sem conversão ao somar séries históricas — precisaria de uma taxa de câmbio histórica por ponto no tempo, que essa change não constrói. Gap conhecido, documentado, não piorado nem consertado aqui.
- Cache/agendamento da taxa de câmbio — busca sob demanda, sem `@Scheduled`, mesma filosofia da cotação de ação. Upgrade path se virar problema de rate-limit: cachear por alguns minutos.
- Interface `Strategy` para múltiplos provedores de câmbio — só existe uma fonte (TwelveData); criar uma interface pra uma única implementação seria abstração sem necessidade.
- SweetAlert2 (confirmações/alertas/toasts) — pedido separado do usuário, brainstorm próprio depois desta feature.

## Capabilities

### New Capabilities
- `conversao-cambio-eua`: conversão USD→BRL de operações e cotações de ações do mercado EUA, com taxa gravada de forma imutável na operação (nunca recalculada com taxa atual) e exibição em reais ao lado do valor original.

### Modified Capabilities

## Impact

- Backend: `model/Operacao.java`, `integration/TwelveDataCambioClient.java` (novo), `service/SaldoCalculator.java`, `service/OperacaoService.java`, `dto/AcaoResponseDTO.java`, `dto/OperacaoResponseDTO.java`, `service/AcaoService.java`.
- Testes: `SaldoCalculatorTest` (caso com `taxaCambio` != 1), `OperacaoIntegrationTest` (compra EUA grava taxa e desconta valor convertido, mock de `TwelveDataCambioClient`), `AcaoServiceTest` (`cotacaoAtualBRL` populado/`null`/degradação graciosa).
- Frontend: `static/js/dashboard.js` (helpers de conversão + aplicação nas 4 telas + fix de mistura de moeda no "Não realizado"), CSS (`.valor-convertido`).
- Sem migração de banco necessária (`spring.jpa.hibernate.ddl-auto=create-drop` em dev).
- Já implementado na branch `feature-conversao-cambio-eua`. Esta change documenta retroativamente o que já está pronto, a partir de `docs/superpowers/specs/2026-08-19-conversao-cambio-eua-design.md` e `docs/superpowers/plans/2026-08-19-conversao-cambio-eua.md`.
