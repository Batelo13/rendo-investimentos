# Conversão de Câmbio (USD→BRL) para Ações Americanas — Design

Data: 2026-08-19
Branch prevista: nova branch a partir de `main` (depois que o PR #18 for mergeado)

## Contexto

Ações do mercado `EUA` (`Acao.mercado`) já têm `moeda` guardada (`"USD"`) e `cotacaoAtual` na moeda de origem, obtida via `TwelveDataCotacaoProvider`. O usuário quer ver, ao lado do valor em dólar, o equivalente em reais — tanto no painel (posições que já tem) quanto no momento de comprar/vender — porque o saldo virtual da carteira é todo em BRL.

**Bug pré-existente encontrado durante a exploração, confirmado com o usuário que deve ser corrigido junto:** hoje `SaldoCalculator`/`OperacaoService.registrar()` tratam `precoUnitario × quantidade` como reais direto, sem nenhuma conversão, mesmo para ações EUA. Comprar 10 ações a US$310,49 desconta "R$3.104,90" do saldo (em vez do equivalente real em reais). A mensagem de erro de saldo insuficiente também mostra esse número bruto rotulado como R$.

## Escopo

- Corrigir o cálculo de saldo (`SaldoCalculator`) para converter operações em ações EUA pela taxa de câmbio vigente **no momento de cada operação**, gravada de forma imutável na própria `Operacao` — mesma filosofia já usada pro histórico de operações (fonte da verdade determinística, nunca recalculada com dado "de agora").
- Mostrar o valor convertido em reais ao lado do valor em dólar em três lugares: catálogo de Ações, painel de posições (Visão Geral + Minhas Posições), e modal de compra/venda.
- Nova integração `TwelveDataCambioClient` (mesma API/chave já usada pra cotação de ações EUA).

Fora de escopo (não construir agora):

- **Gráfico de Rendimento** (`RendimentoService`): hoje também mistura USD e BRL sem conversão ao somar `valorAtual - valorInvestido` de todas as posições pra calcular o "não realizado" ao longo do tempo. Corrigir isso direito exigiria uma taxa de câmbio *histórica* em cada ponto no tempo (equivalente a um `HistoricoCotacao` para câmbio), que essa feature não constrói. Fica marcado como gap conhecido, não piorado nem consertado aqui.
- SweetAlert2 (confirmações, alertas de erro, toasts) — pedido separado do usuário, vira um brainstorm próprio depois desta feature.
- Cache/agendamento da taxa de câmbio — mesma filosofia já usada pra cotação de ação (`HistoricoCotacao`): busca sob demanda, sem `@Scheduled`. Upgrade path se virar problema de rate-limit: cachear por alguns minutos.
- Interface `Strategy` para múltiplos provedores de câmbio — só existe uma fonte (TwelveData), diferente de `CotacaoProvider` que genuinamente precisa escolher entre BRASIL/EUA. Criar uma interface pra uma única implementação seria abstração sem necessidade.

## Modelo de dados

### `Operacao`

Novo campo:

- `taxaCambio` (`BigDecimal`, `nullable = false`) — `1` para operações em ações BRASIL (nunca busca taxa nenhuma), taxa USD→BRL vigente no momento do registro para operações em ações EUA. Sem migração necessária (`ddl-auto=create-drop` em dev).

## Integração: `TwelveDataCambioClient` (novo)

Componente simples (`integration/TwelveDataCambioClient.java`), sem interface — não há Strategy a resolver, só uma fonte:

```java
@Component
public class TwelveDataCambioClient {
    public BigDecimal buscarTaxaUsdParaBrl() { ... }
}
```

- `GET https://api.twelvedata.com/exchange_rate?symbol=USD/BRL&apikey=...` (mesma `RestClient`/timeout/chave já configurados em `TwelveDataCotacaoProvider` — `twelvedata.api.key`).
- Falha (rede, chave inválida, símbolo não encontrado) → `ServicoExternoIndisponivelException`, mesmo padrão dos outros clientes externos.

## Mudanças em componentes existentes

### `SaldoCalculator`

```java
BigDecimal valor = operacao.getPrecoUnitario()
        .multiply(operacao.getQuantidade())
        .multiply(operacao.getTaxaCambio());
```

Único ponto de mudança — como `taxaCambio` é sempre `1` para ações BRASIL, o comportamento pra elas fica idêntico ao de hoje.

### `OperacaoService.registrar()`

Antes de montar a `Operacao`:

```java
BigDecimal taxaCambio = acao.getMercado() == Mercado.EUA
        ? cambioClient.buscarTaxaUsdParaBrl()
        : BigDecimal.ONE;
operacao.setTaxaCambio(taxaCambio);
```

`custoCompra` (checagem de saldo insuficiente) passa a multiplicar por `taxaCambio` também. A mensagem de erro passa a mostrar o valor já convertido (`"...tentando comprar R$ " + custoCompraConvertido`).

**Trade-off aceito:** registrar uma operação em ação EUA agora depende de uma chamada externa (antes, `registrar()` não fazia nenhuma). Se a TwelveData estiver fora do ar, a compra/venda falha com 503 (`ServicoExternoIndisponivelException`) — comportamento correto aqui, porque sem a taxa não dá pra saber quanto cobrar de verdade. Diferente do caminho de exibição (abaixo), que degrada graciosamente.

### `AcaoResponseDTO`

Novo campo `cotacaoAtualBRL` (`BigDecimal`, nullable):

```java
public record AcaoResponseDTO(
        Long id, String ticker, String nomeEmpresa, Mercado mercado, String moeda,
        BigDecimal cotacaoAtual, BigDecimal cotacaoAtualBRL, LocalDateTime dataHoraCotacao
) {}
```

- `null` para ações BRASIL, ou ações EUA sem `cotacaoAtual` ainda, ou se a busca da taxa falhar.
- Para ações EUA com `cotacaoAtual` presente: `cotacaoAtual.multiply(taxa).setScale(2, RoundingMode.HALF_UP)`.

### `AcaoService`

- `listar()`: busca a taxa de câmbio **uma única vez** (não por ação) só se houver pelo menos uma ação EUA na lista resultante; se a busca falhar, loga e segue com `cotacaoAtualBRL = null` em todas — é um dado de exibição secundário, não deve derrubar a listagem inteira.
- `buscarPorId`/`buscarPorTicker`/`criar`/`atualizarCotacao`: mesma lógica, uma ação por vez — se for EUA, tenta buscar a taxa (mesma degradação graciosa em caso de falha).

## Frontend (`dashboard.js`)

Nenhum campo novo em `PosicaoDTO`/`OperacaoResponseDTO` — a taxa vem embutida em `acao.cotacaoAtualBRL` (já carregado em `state.acoes`) e o frontend deriva o resto:

```javascript
function taxaCambioPorTicker(ticker) {
    const a = state.acoes.find((x) => x.ticker === ticker);
    if (!a || a.moeda !== "USD" || a.cotacaoAtualBRL == null || !a.cotacaoAtual) return null;
    return Number(a.cotacaoAtualBRL) / Number(a.cotacaoAtual);
}

function fmtConvertido(valorNaMoedaOriginal, ticker) {
    const taxa = taxaCambioPorTicker(ticker);
    if (taxa == null || valorNaMoedaOriginal == null) return "";
    return `<span class="valor-convertido">≈ ${esc(fmtMoeda(Number(valorNaMoedaOriginal) * taxa, "BRL"))}</span>`;
}
```

Aplicado (sempre como uma segunda linha/elemento junto do valor original, nunca substituindo):

- **Catálogo de Ações** (tabela): célula de cotação atual ganha `fmtConvertido(a.cotacaoAtual, a.ticker)` embaixo.
- **Visão Geral** (mini-lista) e **Minhas Posições** (tabela): preço médio e valor atual ganham a linha convertida.
- **Modal de compra/venda** (`ligarResumoOperacao`): "Valor total" e "Resultado estimado" ganham a linha convertida — é o momento do exemplo do usuário ("comprei a US$310,49, mostra que vou pagar ≈R$1.618,46").
- CSS: `.valor-convertido` — texto menor, cor secundária (`--rendo-color-text-secondary` ou equivalente já existente nos tokens), só uma linha discreta abaixo do valor principal.

**Visão Geral, card "Não realizado":** hoje soma `valorAtual - valorInvestido` de todas as posições sem converter (bug de mistura de moeda também pré-existente, igual ao do saldo). Corrigido de graça aqui: a soma passa a converter cada posição EUA pela sua taxa antes de somar, usando o mesmo `taxaCambioPorTicker`.

## Erros e validações

- Falha ao buscar taxa durante `registrar()` de operação EUA: 503, mesma exceção/formato já usado pelos outros clientes externos.
- Falha ao buscar taxa durante leitura (`GET /acoes`, `GET /carteiras/me`): não falha a request — `cotacaoAtualBRL` fica `null`, frontend simplesmente não mostra a linha convertida pra aquela ação (`fmtConvertido` já trata `taxa == null`).

## Testes

- **`SaldoCalculatorTest`**: novo caso com uma operação de ação EUA com `taxaCambio` diferente de 1, confirmando que o saldo é descontado/creditado no valor convertido, não no bruto.
- **`OperacaoIntegrationTest`**: novo cenário — compra de ação EUA registra `taxaCambio` != 1 na `Operacao` salva e desconta o saldo convertido (mock do `TwelveDataCambioClient`, mesmo padrão de mock já usado pros outros clientes externos nos testes existentes).
- **`AcaoServiceTest`** (ou equivalente): `cotacaoAtualBRL` populado pra ação EUA, `null` pra ação BRASIL, `null` quando a busca de taxa falha (degradação graciosa).

## Self-Review

**Placeholder scan:** nenhum "TBD"/"TODO" — as decisões de escopo (corrigir saldo, fonte do câmbio, taxa gravada na operação, exibição sem campo novo em PosicaoDTO) vieram das perguntas feitas ao usuário e estão resolvidas aqui.

**Consistência interna:** `taxaCambio` segue o mesmo padrão de "campo controlado pelo sistema, nunca pelo cliente" já usado em `Usuario.role`/`Acao.cotacaoAtual` — nunca vem do `OperacaoRequestDTO`, sempre calculado no service.

**Fora de escopo, e por quê:** gráfico de Rendimento (precisaria de taxa histórica por ponto no tempo, feature maior, não pedida); SweetAlert2 (pedido não relacionado, vira brainstorm separado); cache de taxa (YAGNI até virar problema real de rate-limit, mesmo raciocínio já usado pra cotação de ação).
