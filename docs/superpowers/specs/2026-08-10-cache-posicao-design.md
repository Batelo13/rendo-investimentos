# Cache de Posição Materializada — Design

Data: 2026-08-10
Branch prevista: `10-cache-posicao`

## Contexto

`Carteira` já existe (feature anterior): histórico imutável de `Operacao` (compra/venda) e
posição atual **calculada a partir desse histórico**, nunca armazenada — decisão deliberada na
época, para garantir consistência por construção. `CarteiraService.buscarPosicaoPorUsuarioId`
hoje busca todas as `Operacao` ATIVAs da carteira, agrupa por ação+corretora em memória, e roda
`PosicaoCalculator` em cada grupo a cada chamada de `GET /carteiras/me` ou `GET
/carteiras/{usuarioId}`.

Motivação desta feature: exercício/aprendizado do padrão de cache materializado, não uma dor de
performance real observada hoje. `Operacao` continua sendo a única fonte da verdade;
`PosicaoAtual` é um índice derivado por cima dela, nunca a origem de um cálculo.

## Escopo

- Nova tabela `PosicaoAtual`, cache da posição corrente por carteira+ação+corretora.
- Cache escrito **na mesma transação** de `OperacaoService.registrar()` e `.cancelar()` — nunca
  fica desatualizado, porque não existe nenhum outro caminho de escrita em `Operacao`.
- `CarteiraService.buscarPosicaoPorUsuarioId` passa a ler do cache em vez de recalcular do
  histórico a cada chamada.
- Endpoint de ADMIN para reconstruir o cache de uma carteira a partir do histórico completo —
  rede de segurança caso cache e histórico algum dia divirjam.

Fora de escopo (adiar para depois, não construir agora):

- Reconstrução em lote de todas as carteiras de uma vez (só uma carteira por chamada).
- Cache de `valorInvestido`/`valorAtual` — esses dois continuam calculados na leitura,
  multiplicando `PosicaoAtual.quantidade`/`.precoMedio` pela cotação atual da ação. Cachear
  esses dois valores os deixaria desatualizados toda vez que a cotação mudasse sem nenhuma
  operação nova naquele ação+corretora.
- Qualquer forma de invalidação assíncrona/eventual (fila, evento, job). A escrita síncrona no
  mesmo commit já garante consistência forte sem essa complexidade.

## Modelo de dados

### `PosicaoAtual`

Cache materializado, uma linha por carteira+ação+corretora com posição líquida diferente de
zero histórico (ver seção "Regra: quando não há linha").

- `id`
- `carteira` (`@ManyToOne`)
- `acao` (`@ManyToOne`)
- `corretora` (`@ManyToOne`)
- `quantidade` (`BigDecimal`) — mesmo campo/tipo de `PosicaoCalculator.Posicao.quantidade`
  (suporta ações fracionárias, feature já existente).
- `precoMedio` (`BigDecimal`)
- `atualizadoEm` (`LocalDateTime`)

Constraint única em `(carteira_id, acao_id, corretora_id)` — mesmo padrão de unicidade já usado
em `Carteira.usuario_id` e `Corretora.cnpj`.

**Regra: quando não há linha.** Uma linha só existe depois que pelo menos uma operação daquele
ação+corretora foi registrada. Uma vez criada, a linha nunca é apagada mesmo que a posição
zere (venda total) — fica com `quantidade = 0`, mesmo comportamento que o cálculo ao vivo já
tinha (`PosicaoCalculator` retorna quantidade zero, e o código de leitura já filtrava isso). Ou
seja: "não existe linha" e "existe linha com quantidade zero" são tratados da mesma forma na
leitura (nenhum dos dois aparece na resposta de `GET /carteiras/me`) — a diferença é só interna,
não observável pela API.

## `PosicaoCacheService` (novo)

Único responsável por escrever `PosicaoAtual`. Não expõe leitura — quem lê é
`CarteiraService`/`PosicaoAtualRepository` diretamente.

- `atualizar(Carteira carteira, Acao acao, Corretora corretora)`: busca o histórico ATIVA
  daquele carteira+ação+corretora (repository method já existente,
  `findByCarteiraIdAndAcaoIdAndCorretoraIdAndStatusOrderByDataHoraAsc`), roda
  `PosicaoCalculator.calcular`, e faz upsert (`findByCarteiraIdAndAcaoIdAndCorretoraId` — busca
  a linha existente ou cria uma nova) com `quantidade`, `precoMedio`, `atualizadoEm = now()`.
  Chamado por `OperacaoService.registrar()` e `.cancelar()`, sempre depois de salvar a
  `Operacao`, na mesma transação `@Transactional` que já existe nesses dois métodos.
- `reconstruirCarteira(Carteira carteira)`: apaga todas as linhas de `PosicaoAtual` daquela
  carteira, busca todo o histórico ATIVA da carteira (`findByCarteiraIdAndStatus`, já existe),
  agrupa por ação+corretora (mesma lógica de agrupamento que hoje vive em
  `CarteiraService.buscarPosicaoPorUsuarioId`, que migra pra cá), e chama `atualizar` pra cada
  grupo. Reconstrução completa, não incremental — sempre confiável mesmo que o cache estivesse
  arbitrariamente errado antes.

## Mudanças em componentes existentes

### `OperacaoService`

Ganha dependência de `PosicaoCacheService`. `registrar()` e `cancelar()` chamam
`posicaoCacheService.atualizar(carteira, acao, corretora)` logo após `operacaoRepository.save(...)`,
antes do `return`. Nenhuma outra lógica de negócio muda.

### `CarteiraService`

`buscarPosicaoPorUsuarioId(usuarioId)`: deixa de chamar `operacaoRepository` +
`PosicaoCalculator`. Passa a ser `posicaoAtualRepository.findByCarteiraId(carteira.getId())`,
filtrando `quantidade > 0` (mesmo corte que já existia) e montando `PosicaoDTO` com
`valorInvestido = precoMedio × quantidade` e `valorAtual = cotacaoAtual × quantidade` — os dois
únicos campos que continuam calculados na leitura.

Ganha `reconstruirPosicao(usuarioId)`: busca a `Carteira` do usuário (mesmo padrão de
`RecursoNaoEncontradoException` já usado nos outros métodos), delega pra
`posicaoCacheService.reconstruirCarteira(carteira)`, e retorna a posição recém-recalculada
chamando o próprio `buscarPosicaoPorUsuarioId` em seguida.

### `CarteiraController`

Novo endpoint:

```
PATCH /carteiras/{usuarioId}/reconstruir
```

Retorna `List<PosicaoDTO>` (a posição recalculada), mesmo padrão de `OperacaoController.cancelar`
que retorna o recurso atualizado em vez de `204 No Content`.

### `SecurityConfig`

Novo matcher, mesmo padrão de `PATCH /operacoes/*/cancelar`:

```java
.requestMatchers(HttpMethod.PATCH, "/carteiras/*/reconstruir").hasRole("ADMIN")
```

Precisa vir **antes** do matcher genérico `GET /carteiras/*` (que é só GET, não colide por
método HTTP, mas mantém o mesmo cuidado de ordenação já documentado no `SecurityConfig` para
`/carteiras/me`).

## Erros e validações

Nenhuma regra de negócio nova. `PosicaoCacheService.atualizar` e `.reconstruirCarteira` não
validam nada — todas as validações (corretora validada na CVM, saldo suficiente pra venda,
saldo mínimo histórico pra cancelamento) já aconteceram em `OperacaoService` antes da chamada.
O cache reflete um histórico que já é válido por construção; ele nunca pode ficar inconsistente
por causa de uma regra de negócio violada, só por um bug de sincronização — e é exatamente esse
cenário que o endpoint de reconstrução cobre.

`reconstruirPosicao` de um `usuarioId` inexistente: `RecursoNaoEncontradoException` (404), mesmo
comportamento que `buscarPosicaoPorUsuarioId` já tem hoje.

## Testes

- **Regressão de graça:** os 9 cenários já existentes em `OperacaoIntegrationTest` fazem `POST
  /operacoes` seguido de `GET /carteiras/me` — como a leitura passa a vir do cache, esses testes
  continuando verdes é evidência direta de que o cache está sendo escrito certo em cada caso
  (compra simples, venda com preço médio, venda a descoberto bloqueada, corretora não validada,
  cancelamento, cancelamento bloqueado por saldo negativo, zerar-e-recomeçar, fracionário).
- **Novo teste de reconstrução:** registra operações, lê a posição via `GET /carteiras/me`,
  apaga a linha de `PosicaoAtual` correspondente direto pelo `PosicaoAtualRepository` (simulando
  divergência), chama `PATCH /carteiras/{id}/reconstruir`, confirma que a posição retornada bate
  com o valor original.
- **Novo teste de autorização:** usuário comum chamando `PATCH /carteiras/{id}/reconstruir`
  recebe 403, mesmo padrão dos outros testes de autorização já existentes no arquivo.

## Self-Review

**Placeholder scan:** nenhum "TBD"/"TODO" — todas as decisões (o que cachear, quando escrever,
escopo do rebuild) vieram das perguntas de brainstorming e estão resolvidas neste documento.

**Consistência interna:** `PosicaoAtual.quantidade`/`.precoMedio` usam os mesmos tipos e nomes
que `PosicaoCalculator.Posicao` e `PosicaoDTO` já usam — sem conversão de nome nem de tipo entre
camadas.

**Fora de escopo, e por quê:** rebuild em lote (YAGNI — um endpoint por carteira já cobre o caso
de uso real, que é "esse usuário específico parece com posição errada"); cache de
valorInvestido/valorAtual (ficaria desatualizado pela cotação, que muda fora do fluxo de
operações — argumento já registrado na seção de Escopo).
