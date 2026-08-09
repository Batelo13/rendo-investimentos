# Carteira de Investimentos — Design

Data: 2026-08-09
Branch prevista: `6-carteira`

## Contexto

O projeto Rendo já tem `Usuario` (auth), `Corretora` (cadastro validado por CNPJ) e `Acao`
(catálogo com cotação via Strategy). Nenhuma dessas entidades se conecta hoje. `Carteira` é o
domínio que amarra as três: registra as operações de compra/venda de um usuário, numa
corretora, para uma ação, e deriva a posição atual dele.

## Escopo

- Histórico de operações (compra/venda) **e** posição atual (saldo), não uma versão
  simplificada de um dos dois.
- Posição é **calculada a partir do histórico**, não armazenada numa tabela separada — sempre
  consistente por construção, sem lógica de reconciliação.

Fora de escopo (adiar para depois, não construir agora):

- Ações fracionárias (quantidade é sempre inteira).
- Edição/exclusão de operação pelo usuário comum (histórico é imutável para quem não é ADMIN).
- Múltiplas carteiras por usuário (relação é 1:1).
- Endpoint de bloqueio temporário de usuário — já existe o campo `Usuario.ativo` usado no
  login; falta só um endpoint de ADMIN pra alternar esse campo. É uma feature pequena e
  independente do domínio `Carteira`; deve ser feita à parte.
- Tabela de posição materializada/cache — se o volume um dia justificar, dá pra adicionar por
  cima do mesmo histórico sem migrar nada.

## Modelo de dados

### `Carteira`

1:1 com `Usuario`. Criada automaticamente na mesma transação de `UsuarioService.cadastrar` —
usuário nunca fica sem carteira, não existe endpoint de criação.

- `id`
- `usuario` (`@OneToOne`)
- `dataCriacao`

### `Operacao`

Registro histórico e imutável de uma compra ou venda.

- `id`
- `carteira` (`@ManyToOne`)
- `acao` (`@ManyToOne`)
- `corretora` (`@ManyToOne`)
- `tipo`: enum `TipoOperacao { COMPRA, VENDA }`
- `quantidade`: `Integer`, positivo
- `precoUnitario`: `BigDecimal`, positivo — preço pelo qual a operação foi executada,
  informado pelo cliente (não é copiado de `Acao.cotacaoAtual`, que é só a cotação "ao vivo"
  no catálogo; a operação pode ter acontecido a outro preço)
- `dataHora`: `LocalDateTime`, preenchida pelo servidor no momento do registro — nunca vem do
  cliente, mesmo padrão de `dataCadastro` em `Usuario`/`Corretora`
- `precoMedioNaVenda`: `BigDecimal`, nulo para `COMPRA`. Para `VENDA`, é o preço médio da
  posição **imediatamente antes** dessa venda, calculado e gravado no momento da criação da
  operação. Fica congelado no histórico para sempre — operações futuras (novas compras) não
  o recalculam retroativamente.
- `status`: enum `StatusOperacao { ATIVA, CANCELADA }`
- `canceladaEm`: `LocalDateTime`, nulo até uma ADMIN cancelar
- `canceladaPor`: `Usuario` (a ADMIN que cancelou), nulo até cancelar

`lucroPrejuizoRealizado` **não** é um campo persistido: é derivado na resposta a partir de
`quantidade × (precoUnitario − precoMedioNaVenda)`, só faz sentido pra `VENDA`.

## Regras de negócio

### Cálculo de posição e preço médio

A posição de um usuário numa `acao`+`corretora` é calculada processando as `Operacao`s `ATIVA`s
**em ordem cronológica** (não é uma soma simples e desordenada — isso importa pro próximo
ponto):

- `COMPRA`: `novaQuantidade = quantidade + qtdComprada`; `novoPrecoMedio = (quantidade ×
  precoMedio + qtdComprada × precoCompra) / novaQuantidade`.
- `VENDA`: `novaQuantidade = quantidade − qtdVendida`; **o preço médio não muda** — vender não
  altera o custo médio de quem ficou na carteira, isso é como custo médio ponderado funciona.
- Se `novaQuantidade` chega a zero, o preço médio é resetado (indefinido). Uma compra
  seguinte começa uma média nova, do zero.

Esse último ponto corrige um bug conhecido de implementações ingênuas (`soma de todas as
compras ÷ saldo líquido atual`): sem o reset explícito no zero, uma posição que foi zerada e
recomeçada "herda" preço médio de um lote que já não existe mais na carteira. Exemplo: compra
10 @ R$10, vende as 10 (saldo 0), compra 5 @ R$20 — a fórmula ingênua dá R$40 de preço médio;
o correto é R$20.

### Preço médio na venda (`precoMedioNaVenda`)

Ao registrar uma `VENDA`, o servidor roda o cálculo de posição acima **até aquele instante**
(sem incluir a própria venda) para obter o preço médio vigente, grava esse valor em
`precoMedioNaVenda` da operação, e só então aplica a baixa de quantidade. Esse valor aparece
tanto na resposta imediata do `POST /operacoes` quanto em todo o histórico depois — é isso que
permite ver, pra sempre, "vendi 5 AAPL a R$50, preço médio na hora era R$100" mesmo que compras
futuras mudem o preço médio atual da carteira.

### Validações

- `VENDA` é bloqueada se `quantidade` pedida for maior que o saldo atual calculado (não vende
  a descoberto).
- Operação só pode ser registrada numa `Corretora` com `validadaNaCvm = true`.
- ADMIN só pode cancelar operação com `tipo = COMPRA`.
- Cancelamento é bloqueado se deixaria a quantidade líquida negativa (mesma lógica de "não
  vende a descoberto", aplicada ao cancelamento) — ex: comprou 10, vendeu 8, não pode cancelar
  a compra das 10 porque sobrariam -8.
- Cancelar uma `COMPRA` **não** recalcula `precoMedioNaVenda` já gravado em vendas passadas —
  histórico é fato consumado.

### Imutabilidade

Usuário comum só cria operações (`COMPRA` ou `VENDA`); não edita nem exclui. ADMIN só pode
cancelar (soft-delete, `status = CANCELADA`) uma `COMPRA` — nenhuma outra escrita.

## Endpoints e autorização

| Método | Rota | Quem | O que faz |
|---|---|---|---|
| `POST` | `/operacoes` | qualquer autenticado | Registra `COMPRA` ou `VENDA` na própria carteira |
| `GET` | `/carteiras/me` | qualquer autenticado | Posição atual (agrupada por ação+corretora): quantidade, preço médio, valor investido, valor atual (via `Acao.cotacaoAtual`) |
| `GET` | `/carteiras/me/operacoes` | qualquer autenticado | Histórico de operações da própria carteira |
| `GET` | `/carteiras/{usuarioId}` | ADMIN | Posição de qualquer usuário (somente leitura) |
| `GET` | `/carteiras/{usuarioId}/operacoes` | ADMIN | Histórico de qualquer usuário (somente leitura) |
| `PATCH` | `/operacoes/{id}/cancelar` | ADMIN | Cancela uma operação `COMPRA` |

Usuário comum nunca informa o id da própria carteira/usuário no corpo da requisição — sempre
vem do contexto de autenticação (`SecurityContext`), mesmo padrão já usado em `Usuario` pra
`role`/`ativo`. Tentativa de um usuário comum acessar `/carteiras/{outroId}` retorna `403`
(Spring Security, mesmo mecanismo já usado em `GET /usuarios`).

Nas rotas `/carteiras/*`, o matcher `/carteiras/me/**` precisa ser declarado **antes** do
matcher `/carteiras/*` restrito a ADMIN no `SecurityConfig` — do contrário `/carteiras/me`
seria capturado pelo matcher genérico e exigiria ADMIN por engano.

## Tratamento de erros

Nova `RegraDeNegocioException`, seguindo o padrão já existente em `GlobalExceptionHandler`
(`RecursoNaoEncontradoException`, `RecursoDuplicadoException`, etc.), cobre:

- Venda a descoberto
- Corretora não validada na CVM
- Cancelamento que negativaria o saldo
- Cancelar operação que não é `COMPRA`

`RecursoNaoEncontradoException` (já existe) cobre `acaoId`/`corretoraId`/`operacaoId`
inválidos. `403` continua sendo o mecanismo padrão do Spring Security pra acesso não
autorizado a carteira alheia.

## Componentes

- `Carteira` (entidade) + `CarteiraRepository`
- `Operacao` (entidade) + `OperacaoRepository`
- `TipoOperacao`, `StatusOperacao` (enums)
- `PosicaoCalculator`: dado o histórico de operações `ATIVA`s de uma acao+corretora em ordem
  cronológica, calcula quantidade e preço médio (algoritmo da seção "Cálculo de posição"
  acima). Componente isolado e testável por si só — é reutilizado tanto para "posição atual"
  quanto para "preço médio no instante de uma venda" (mesma lógica, cortando o histórico até
  um ponto no tempo).
- `CarteiraService`: cria a carteira automaticamente (chamado a partir de
  `UsuarioService.cadastrar`), monta a posição agregada pra `GET /carteiras/me` e
  `GET /carteiras/{usuarioId}`.
- `OperacaoService`: registra operação (validações de negócio), lista histórico, cancela.
- `CarteiraController`: `GET /carteiras/me`, `GET /carteiras/{usuarioId}`.
- `OperacaoController`: `POST /operacoes`, `GET /carteiras/me/operacoes`,
  `GET /carteiras/{usuarioId}/operacoes`, `PATCH /operacoes/{id}/cancelar`.

### DTOs

- `OperacaoRequestDTO`: `acaoId`, `corretoraId`, `tipo`, `quantidade` (`@Positive`),
  `precoUnitario` (`@Positive`).
- `OperacaoResponseDTO`: `id`, `tipo`, `quantidade`, `precoUnitario`, `dataHora`, `status`,
  `acaoTicker`, `corretoraNome`, `precoMedioNaVenda` (nulo em compras),
  `lucroPrejuizoRealizado` (derivado, nulo em compras).
- `PosicaoDTO`: `acaoTicker`, `corretoraNome`, `quantidade`, `precoMedio`, `valorInvestido`
  (`quantidade × precoMedio`), `valorAtual` (`quantidade × Acao.cotacaoAtual`).

## Testes

Testes de integração no mesmo estilo de `UsuarioAuthIntegrationTest` (MockMvc + Spring
Security real, `@SpringBootTest` + `@AutoConfigureMockMvc`):

- Registra compra; registra venda parcial e confere `precoMedioNaVenda` no histórico.
- Bloqueia venda maior que o saldo atual (venda a descoberto).
- Bloqueia operação em corretora com `validadaNaCvm = false`.
- Usuário não acessa carteira/operações de outro usuário (`403`).
- ADMIN acessa carteira de qualquer usuário (`200`, somente leitura).
- ADMIN cancela uma `COMPRA`; usuário comum não consegue (`403`).
- Cancelamento bloqueado quando deixaria saldo negativo.
- Cenário do preço médio "herdado" incorretamente: zera a posição vendendo tudo, compra de
  novo a um preço diferente, preço médio tem que recomeçar do zero — não misturar com o lote
  anterior.
