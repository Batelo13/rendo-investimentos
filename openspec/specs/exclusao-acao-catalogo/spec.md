## Purpose

Permite corrigir um cadastro de ação equivocado no catálogo, removendo-a quando ela ainda não foi usada em nenhuma operação de compra/venda, liberando o ticker para ser recadastrado corretamente.

## Requirements

### Requirement: Exclusão de ação sem operações ou posições associadas
O sistema SHALL permitir excluir uma ação do catálogo via `DELETE /acoes/{id}` quando não existir nenhuma operação de compra/venda nem posição atual (de nenhum usuário) associada a essa ação, removendo também seu histórico de cotações.

#### Scenario: Excluir ação nunca comprada
- **WHEN** um usuário autenticado faz `DELETE /acoes/{id}` para uma ação que nunca teve uma operação de compra ou venda registrada
- **THEN** o sistema remove a ação e seu histórico de cotações, e retorna sucesso (204)

#### Scenario: Ticker liberado após exclusão
- **WHEN** uma ação é excluída com sucesso
- **THEN** o mesmo ticker pode ser cadastrado novamente via `POST /acoes`, inclusive com um mercado diferente do anterior

### Requirement: Bloqueio de exclusão de ação com histórico financeiro
O sistema SHALL rejeitar a exclusão de uma ação (HTTP 422) quando existir pelo menos uma operação de compra/venda ou uma posição atual associada a ela, de qualquer usuário.

#### Scenario: Tentativa de excluir ação já comprada
- **WHEN** um usuário faz `DELETE /acoes/{id}` para uma ação que possui ao menos uma operação de compra ou venda registrada (própria ou de outro usuário)
- **THEN** o sistema rejeita a exclusão com um erro de regra de negócio, sem remover a ação nem seu histórico

#### Scenario: Ação inexistente
- **WHEN** um usuário faz `DELETE /acoes/{id}` para um id que não existe no catálogo
- **THEN** o sistema retorna um erro de recurso não encontrado

### Requirement: Exclusão de ação na interface do dashboard
O sistema SHALL exibir um botão "Excluir" para cada ação listada no catálogo do dashboard, pedindo confirmação do usuário antes de efetivar a exclusão.

#### Scenario: Confirmar exclusão
- **WHEN** o usuário clica em "Excluir" em uma ação do catálogo e confirma a ação no diálogo de confirmação
- **THEN** a ação é removida da lista e uma notificação de sucesso é exibida

#### Scenario: Cancelar exclusão
- **WHEN** o usuário clica em "Excluir" mas cancela o diálogo de confirmação
- **THEN** nenhuma requisição de exclusão é enviada e a ação permanece no catálogo

#### Scenario: Exclusão bloqueada pelo backend
- **WHEN** o usuário confirma a exclusão de uma ação que possui operações/posições associadas
- **THEN** a interface exibe a mensagem de erro retornada pelo backend explicando por que a exclusão não é permitida, e a ação permanece no catálogo
