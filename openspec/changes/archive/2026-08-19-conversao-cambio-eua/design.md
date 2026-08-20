## Context

Ver proposal.md - Why. Detalhamento completo do design original (exploração do bug, alternativas descartadas): `docs/superpowers/specs/2026-08-19-conversao-cambio-eua-design.md`. Este documento resume as decisões técnicas para o registro OpenSpec.

## Goals / Non-Goals

**Goals:**
- Corrigir o saldo (hoje descontado sem conversão para ações EUA) usando a taxa de câmbio vigente no momento de cada operação.
- Mostrar o valor convertido em reais ao lado do valor em dólar, sem esconder o valor original.
- Garantir que histórico e cálculos de resultado usem sempre a taxa gravada na própria operação, nunca uma taxa "de agora" recalculada.

**Non-Goals:**
- Corrigir o Gráfico de Rendimento (precisaria de taxa histórica por ponto no tempo — feature maior, não pedida agora).
- Cache/agendamento de taxa de câmbio (YAGNI até virar problema real de rate-limit).
- Interface `Strategy` para múltiplos provedores de câmbio (só existe uma fonte).

## Decisions

**Taxa gravada na `Operacao`, nunca recalculada.** Mesma filosofia já usada pro histórico de cotação (`HistoricoCotacao`): fonte da verdade determinística. `taxaCambio` é `1` para ações BRASIL (nunca busca taxa nenhuma) e a taxa USD→BRL vigente no momento do registro para ações EUA. Alternativa descartada: buscar a taxa atual sempre que exibir uma operação passada — rejeitada porque o valor pago/recebido no passado mudaria toda vez que a taxa de câmbio mudasse, quebrando o histórico.

**`TwelveDataCambioClient` sem interface `Strategy`.** Diferente de `CotacaoProvider`, que genuinamente precisa escolher entre BRASIL/EUA, câmbio só tem uma fonte (TwelveData). Criar uma interface para uma única implementação seria abstração sem necessidade.

**Falha ao buscar taxa se comporta diferente em escrita vs. leitura.** Em `registrar()` (escrita), falha na busca da taxa retorna 503 (`ServicoExternoIndisponivelException`) — sem a taxa não dá pra saber quanto cobrar de verdade, então a operação não pode prosseguir. Em leitura (`GET /acoes`, `GET /carteiras/me`), falha não derruba a request — `cotacaoAtualBRL` fica `null` e o frontend simplesmente não mostra a linha convertida; é um dado de exibição secundário.

**Nenhum campo novo em `PosicaoDTO` para a taxa de exibição.** O frontend deriva a taxa implícita de `acao.cotacaoAtualBRL / acao.cotacaoAtual` (`taxaCambioPorTicker`), reaproveitando o dado que `state.acoes` já carrega — evita duplicar a mesma informação em dois formatos.

**Correção de acompanhamento: histórico/"Não realizado" usam a taxa gravada na operação, não a taxa atual.** Achado durante a verificação manual (Task 9 do plano): o histórico de operações e o card "Não realizado" recalculavam com `cotacaoAtualBRL`/taxa "de agora" em vez da taxa gravada em cada `Operacao`, misturando os dois princípios do design (taxa imutável por operação vs. cotação atual da ação). Corrigido expondo `taxaCambio` em `OperacaoResponseDTO` e ajustando `dashboard.js` para usar o valor já convertido/gravado em vez de recalcular.

## Risks / Trade-offs

[TwelveData fora do ar durante `registrar()` de ação EUA] → Aceito conscientemente: compra/venda falha com 503. Comportamento correto — sem a taxa não dá pra saber quanto cobrar de verdade. Diferente do caminho de exibição, que degrada graciosamente (`cotacaoAtualBRL = null`).

[Gráfico de Rendimento continua misturando moeda] → Gap pré-existente, não piorado nem consertado por esta change. Precisaria de uma taxa de câmbio histórica por ponto no tempo (equivalente a um `HistoricoCotacao` para câmbio) para ser corrigido direito — marcado como trabalho futuro.
