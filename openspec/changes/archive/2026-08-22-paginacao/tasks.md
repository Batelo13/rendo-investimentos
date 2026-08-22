## 1. Ações

- [x] 1.1 `AcaoService.listar()` passa a receber `Pageable` e retornar `Page<AcaoResponseDTO>` (`repository.findAll(pageable)` + `.map(...)`)
- [x] 1.2 `AcaoController.listar()` recebe `@PageableDefault(size = 20) Pageable pageable` e repassa ao service
- [x] 1.3 Atualizar `AcaoServiceTest` para mockar `findAll(any(Pageable.class))` retornando `PageImpl<>(...)` e ler `.getContent()`

## 2. Corretoras

- [x] 2.1 `CorretoraService.listar()` passa a receber `Pageable` e retornar `Page<CorretoraResponseDTO>`
- [x] 2.2 `CorretoraController.listar()` recebe `@PageableDefault(size = 20) Pageable pageable`

## 3. Usuários

- [x] 3.1 `UsuarioService.listar()` passa a receber `Pageable` e retornar `Page<UsuarioResponseDTO>`
- [x] 3.2 `UsuarioController.listar()` recebe `@PageableDefault(size = 20) Pageable pageable`

## 4. Operações

- [x] 4.1 Novo overload em `OperacaoRepository`: `Page<Operacao> findByCarteiraIdOrderByDataHoraDesc(Long carteiraId, Pageable pageable)` (a query sem paginação usada nos cálculos não é tocada; o overload sem `Pageable` foi removido por ficar sem uso)
- [x] 4.2 `OperacaoService.listarPorCarteira` (privado), `listarProprias` e `listarComoAdmin` passam a receber/repassar `Pageable` e retornar `Page<OperacaoResponseDTO>`
- [x] 4.3 `CarteiraController.operacoesProprias`/`operacoesPorUsuario` recebem `@PageableDefault(size = 20) Pageable pageable` — sem `sort` explícito: a ordenação já vem fixa do nome da query (`OrderByDataHoraDesc`), então um `sort` no `Pageable` seria redundante

## 5. Frontend

- [x] 5.1 `dashboard.js`: `carregarTudo()` extrai `.content` de `/acoes` e `/corretoras` (pedindo `size` alto, ex. 1000, para preservar o cache completo usado pelo dropdown/lookup)
- [x] 5.2 `dashboard.js`: extrai página de operações via novo estado `state.operacoesPagina` (número da página atual + `totalPages`), busca `/carteiras/me/operacoes?page=N&size=20`
- [x] 5.3 Adicionar controles de paginação (anterior/próxima + "Página X de Y") abaixo da tabela de Histórico de operações em `dashboard.html`, estilizados com os tokens Rendo já existentes
- [x] 5.4 `renderOperacoes()` e o handler dos botões de paginação atualizados para refletir a página carregada

## 6. Verificação manual no navegador

- [x] 6.1 Catálogo de ações e corretoras continuam completos (dropdown do modal de compra confirmado com uma corretora cadastrada além da página de operações)
- [x] 6.2 Histórico de operações: navegado entre as 2 páginas (20 + 5 itens, 25 operações de teste inseridas via H2 console), saldo disponível bateu com o cálculo sobre as 25 operações completas (não só a página exibida)
- [x] 6.3 `registrar()`/`cancelar()` não foram alterados por esta change (só os métodos de listagem foram tocados) — não houve teste de fluxo de compra/venda ao vivo pela UI nesta verificação, risco de regressão nesse ponto é baixo por não fazerem parte do diff
- [x] 6.4 Console do navegador sem erros novos (só ruído de extensão do Chrome, já visto em sessões anteriores)
