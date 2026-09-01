## Why

Não existe forma de remover uma ação cadastrada por engano no catálogo (ex.: ticker certo mas mercado errado, como aconteceu com INTC cadastrada como BRASIL quando deveria ser EUA). Hoje a única saída é conviver com o cadastro incorreto, já que o ticker é único e não pode ser recadastrado enquanto o registro errado existir.

## What Changes

- Adiciona `DELETE /acoes/{id}`: remove a ação do catálogo (e seu histórico de cotações) quando ela não tem nenhuma operação de compra/venda nem posição registrada por nenhum usuário.
- Se a ação já tiver operações ou posições associadas (de qualquer usuário, já que o catálogo é global), a exclusão é **bloqueada** com erro de regra de negócio (422) — preserva o princípio de histórico imutável já adotado para `Operacao`.
- Adiciona um botão "Excluir" na tabela "Ações" do dashboard (catálogo), com confirmação (SweetAlert2, mesmo padrão de `confirmacao-compra-venda`) antes de efetivar.
- Após exclusão bem-sucedida, o ticker fica livre para ser recadastrado.

## Capabilities

### New Capabilities
- `exclusao-acao-catalogo`: exclusão de uma ação do catálogo, bloqueada quando há operações/posições associadas.

### Modified Capabilities
(nenhuma)

## Impact

- `AcaoController` / `AcaoService`: novo endpoint `DELETE /acoes/{id}`.
- `OperacaoRepository` / `PosicaoAtualRepository`: novos métodos `existsByAcaoId`.
- `HistoricoCotacaoRepository`: novo método para apagar o histórico de cotações da ação junto.
- `src/main/resources/templates/dashboard.html` / `static/js/dashboard.js`: botão "Excluir" na tabela do catálogo de ações, com confirmação e tratamento de erro (regra de negócio bloqueando a exclusão).
- Nenhuma mudança em `SecurityConfig` — `DELETE /acoes/{id}` já cai em `anyRequest().authenticated()`, mesmo nível de acesso do `POST /acoes` existente.
