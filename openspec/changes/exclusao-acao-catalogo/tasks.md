## 1. Backend — verificações de uso

- [x] 1.1 Adicionar `boolean existsByAcaoId(Long acaoId)` em `OperacaoRepository`.
- [x] 1.2 Adicionar `boolean existsByAcaoId(Long acaoId)` em `PosicaoAtualRepository`.
- [x] 1.3 Adicionar `void deleteByAcaoId(Long acaoId)` em `HistoricoCotacaoRepository`.

## 2. Backend — endpoint de exclusão

- [x] 2.1 Em `AcaoService`, criar `excluir(Long id)`: busca a ação (`RecursoNaoEncontradoException` se não existir), verifica `operacaoRepository.existsByAcaoId` e `posicaoAtualRepository.existsByAcaoId` — se qualquer um for `true`, lança `RegraDeNegocioException` com mensagem explicando o bloqueio; caso contrário, apaga o histórico de cotações (`historicoRepository.deleteByAcaoId`) e a ação (`repository.delete`). Anotado `@Transactional` (necessário: sem isso o `deleteByAcaoId` derivado do Spring Data falhava com "No EntityManager with actual transaction available").
- [x] 2.2 Em `AcaoController`, adicionar `@DeleteMapping("/{id}")` retornando `ResponseEntity.noContent().build()` após chamar `service.excluir(id)`.

## 3. Frontend — botão e confirmação

- [x] 3.1 Em `dashboard.js`, adicionar um path "trash" em `icone()`.
- [x] 3.2 Em `renderAcoes()` (`dashboard.js`), adicionar um botão `data-delete-acao="${a.id}"` na coluna de ações do catálogo, ao lado de Comprar/Atualizar/Detalhes.
- [x] 3.3 Criar `async function excluirAcao(id)`: pega a ação em `state.acoes`, mostra confirmação via `Swal.fire` (mesmo padrão de `submitOperacao`) com o ticker no texto, e só chama `DELETE /acoes/{id}` se confirmado; em sucesso mostra toast e recarrega (`carregarTudo()`); em erro (bloqueio de regra de negócio ou outro), mostra a mensagem de erro do backend via toast, sem remover a linha da tabela.
- [x] 3.4 Registrar o listener do botão (`[data-delete-acao]`) no delegador de eventos em `bind()`, chamando `excluirAcao(...)`.

## 4. Verificação manual

- [x] 4.1 Rodar a aplicação localmente, cadastrar uma ação sem comprar, excluí-la pelo botão, confirmar que ela some da lista e que o ticker pode ser recadastrado com o mercado correto.
- [x] 4.2 Comprar uma ação e tentar excluí-la: confirmar que a exclusão é bloqueada com uma mensagem de erro clara, e que a ação permanece no catálogo e na posição do usuário.
- [x] 4.3 Rodar a suíte de testes (`./mvnw test`, com `MAIL_ENABLED=false`) e confirmar que nada quebrou.
