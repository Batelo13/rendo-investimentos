## Why

`GET /acoes`, `GET /corretoras`, `GET /usuarios` e `GET /carteiras/.../operacoes` retornam `List<DTO>` completo, sempre — nunca cortam nem paginam. `Operacao` é histórico imutável (nunca apagado) e cresce sem limite a cada compra/venda, então é o candidato mais real a ficar pesado; os demais crescem mais devagar mas devem seguir o mesmo padrão por consistência.

## What Changes

- `GET /acoes`, `GET /corretoras`, `GET /usuarios`, `GET /carteiras/me/operacoes` e `GET /carteiras/{usuarioId}/operacoes` passam a aceitar parâmetros de paginação (`page`, `size`, `sort`) e retornam um envelope paginado (conteúdo da página + metadados: página atual, total de páginas, total de elementos) em vez de um array simples.
- Página padrão quando nenhum parâmetro é enviado: primeira página, 20 itens.
- Consultas usadas internamente para cálculo de posição/saldo/rendimento (`PosicaoCalculator`, `SaldoCalculator`, `RendimentoService`) **não** são afetadas — continuam lendo o histórico completo, pois dependem da ordem cronológica inteira para o cálculo estar correto.
- **BREAKING**: o corpo de resposta desses 5 endpoints muda de array (`[...]`) para objeto paginado (`{ content: [...], totalPages, totalElements, number, ... }`) — qualquer cliente que espera um array direto precisa ser ajustado.
- Frontend (`dashboard.js`): a tabela "Histórico de operações" ganha navegação real de página (anterior/próxima + indicador). As listas de Ações e Corretoras continuam carregando o catálogo inteiro de uma vez (pedindo uma página grande) porque são usadas como cache completo em outras partes da tela (dropdown de corretora no modal de compra, lookup de moeda por ticker, botões de detalhe) — só o backend ganha suporte a paginação nelas por ora; a UI de página real nessas duas fica fora de escopo desta change.

## Capabilities

### New Capabilities
- `paginacao`: exige que listagens que crescem sem limite (a começar pelo histórico de operações) sejam servidas em páginas, com suporte de paginação disponível também nos demais catálogos (ações, corretoras, usuários).

### Modified Capabilities
(nenhuma — não existe capability de "listagem" documentada anteriormente; o contrato dos endpoints muda, mas nenhuma spec existente descrevia o formato de lista)

## Impact

- `AcaoController`/`AcaoService`, `CorretoraController`/`CorretoraService`, `UsuarioController`/`UsuarioService`: `listar()` passa a receber `Pageable` e retornar `Page<DTO>`.
- `OperacaoRepository`: novo overload de `findByCarteiraIdOrderByDataHoraDesc` aceitando `Pageable`, retornando `Page<Operacao>`. A versão sem paginação usada pelos cálculos internos (`findByCarteiraIdAndStatusOrderByDataHoraAsc`) não muda.
- `CarteiraController`/`OperacaoService`: `operacoesProprias`/`operacoesPorUsuario` e `listarProprias`/`listarComoAdmin` passam a receber `Pageable` e retornar `Page<OperacaoResponseDTO>`.
- `dashboard.js`/`dashboard.html`: `carregarTudo()` ajustado para extrair `.content` dos endpoints agora paginados; nova navegação de página na tabela de operações; Ações/Corretoras pedem uma página grande (`size` alto) para preservar o comportamento atual de cache completo.
- Testes: `AcaoServiceTest` (mocka `findAll()` sem argumento) precisa ser atualizado para `findAll(Pageable)`.
