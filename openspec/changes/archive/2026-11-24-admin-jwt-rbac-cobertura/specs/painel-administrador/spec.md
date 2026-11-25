## ADDED Requirements

### Requirement: Estatisticas de contas para o administrador
`GET /usuarios/estatisticas` SHALL devolver, só para ADMIN, o total de contas, as ativas, as bloqueadas, as com e-mail não verificado, os administradores e as contas criadas nos últimos 30 dias. Um USER recebe 403.

#### Scenario: Contagens refletem o banco
- **WHEN** há contas cadastradas, entre elas uma bloqueada e uma ADMIN, e o ADMIN chama o endpoint
- **THEN** a resposta traz `total`, `ativas`, `bloqueadas`, `emailNaoVerificado`, `administradores` e `novasUltimos30Dias` iguais às contagens do banco

#### Scenario: USER barrado
- **WHEN** um USER chama `GET /usuarios/estatisticas`
- **THEN** o sistema responde 403

### Requirement: Tela de administracao no dashboard
O dashboard SHALL mostrar a navegação "Administração" e o selo "Administrador" somente a usuários ADMIN. A tela exibe as estatísticas, a lista de usuários com ação de bloquear ou desbloquear e a consulta da carteira de cada usuário. Para USER, nada disso aparece no HTML.

#### Scenario: Admin vê a área
- **WHEN** um ADMIN abre `/dashboard`
- **THEN** o HTML traz o item de navegação "Administração" e o selo "Administrador"

#### Scenario: Usuário comum não vê
- **WHEN** um USER abre `/dashboard`
- **THEN** o HTML não traz o item "Administração" nem o selo
