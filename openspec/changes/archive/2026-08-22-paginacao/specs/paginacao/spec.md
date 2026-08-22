## Purpose

Garante que listagens que crescem sem limite (a começar pelo histórico de operações) sejam servidas em páginas, evitando que o servidor precise carregar e transmitir o conjunto inteiro a cada requisição.

## ADDED Requirements

### Requirement: Listagem paginada do histórico de operações
O sistema SHALL paginar o histórico de operações da carteira (própria ou de outro usuário, para admin), aceitando os parâmetros `page` e `size` e retornando, além dos itens da página, o total de páginas e o total de elementos.

#### Scenario: Requisição sem parâmetros de página
- **WHEN** o cliente faz `GET /carteiras/me/operacoes` sem informar `page`/`size`
- **THEN** o sistema retorna a primeira página (20 itens) ordenada da operação mais recente para a mais antiga, junto com o total de páginas e o total de elementos

#### Scenario: Requisição de uma página específica
- **WHEN** o cliente faz `GET /carteiras/me/operacoes?page=1&size=10`
- **THEN** o sistema retorna os itens 11 a 20 (a segunda página, considerando 10 itens por página), mantendo a mesma ordenação

#### Scenario: Cálculo de posição, saldo e rendimento continua sobre o histórico completo
- **WHEN** uma compra ou venda é registrada, ou a posição/saldo/rendimento da carteira é recalculado
- **THEN** o sistema usa o histórico completo de operações ativas, não apenas uma página, para que o resultado do cálculo continue correto independente do tamanho do histórico

### Requirement: Suporte a paginação nos catálogos de ações, corretoras e usuários
O sistema SHALL aceitar os parâmetros `page` e `size` em `GET /acoes`, `GET /corretoras` e `GET /usuarios`, retornando os itens da página solicitada junto com o total de páginas e o total de elementos.

#### Scenario: Requisição sem parâmetros de página
- **WHEN** o cliente faz `GET /acoes`, `GET /corretoras` ou `GET /usuarios` sem informar `page`/`size`
- **THEN** o sistema retorna a primeira página (20 itens) junto com o total de páginas e o total de elementos
