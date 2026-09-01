## 1. Markup dos filtros

- [x] 1.1 Em `dashboard.html`, no `panel-head` da seção `#view-posicoes`, adicionar três `<select>` (mercado, corretora, resultado) ao lado do `#filterPosicoes` existente, com IDs `#filterPosicoesMercado`, `#filterPosicoesCorretora`, `#filterPosicoesResultado`.
- [x] 1.2 Cada `<select>` começa com a opção padrão "Todos" (`value=""`), seguindo o padrão do `<select name="mercado">` já usado no formulário de cadastro de ação.
- [x] 1.3 Estilizar os novos selects reaproveitando as classes existentes (`filter-input` ou equivalente) para manter consistência visual com o restante do dashboard.

## 2. Lógica de filtragem em dashboard.js

- [x] 2.1 Criar função `corretorasDasPosicoes()` que retorna a lista de nomes de corretoras distintas presentes em `state.posicoes`.
- [x] 2.2 Criar função `popularFiltroCorretoras()` que popula `#filterPosicoesCorretora` com as opções de `corretorasDasPosicoes()`, preservando a seleção atual quando ela ainda existir na nova lista.
- [x] 2.3 Criar função `resultadoPosicao(p)` que retorna `"lucro"`, `"prejuizo"` ou `"neutro"` a partir do sinal de `Number(p.valorAtual) - Number(p.valorInvestido)`.
- [x] 2.4 Estender `renderPosicoes()` para ler os valores atuais dos três novos selects e aplicar os filtros em conjunto (AND lógico) com o filtro textual já existente.
- [x] 2.5 Atualizar `$("#countPosicoes")` para refletir o tamanho da lista já filtrada (`lista.length`), em vez do total de `state.posicoes`.
- [x] 2.6 Garantir que o estado vazio (`#emptyPosicoes` / mensagem "Nenhum resultado") já usado pela busca textual também cobre o caso de filtros combinados sem resultado.

## 3. Integração e eventos

- [x] 3.1 Registrar listeners `change` nos três novos selects chamando `renderPosicoes()`, ao lado do listener `input` já existente em `#filterPosicoes`.
- [x] 3.2 Chamar `popularFiltroCorretoras()` sempre que `state.posicoes` for recarregado (mesmo ponto onde `renderPosicoes()` já é chamado após o carregamento inicial).

## 4. Verificação manual

- [x] 4.1 Rodar a aplicação localmente e, na tela "Minhas posições", validar: filtro isolado por mercado, por corretora, por resultado, e a combinação dos três com a busca textual.
- [x] 4.2 Validar o caso de zero posições correspondentes (estado vazio) e o caso de usuário com posições em uma única corretora (select de corretora com uma única opção além de "Todos").
