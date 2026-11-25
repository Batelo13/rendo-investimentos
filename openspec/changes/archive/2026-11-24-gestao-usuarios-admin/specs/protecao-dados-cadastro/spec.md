## MODIFIED Requirements

### Requirement: Papel ADMIN concedido por configuracao
O sistema SHALL conceder papel ADMIN aos emails listados na configuracao do servidor, no
cadastro e na inicializacao. Alem disso, um ADMIN autenticado SHALL poder promover ou rebaixar
outras contas, exceto emails listados na configuracao, que permanecem ADMIN.

#### Scenario: Email configurado como administrador
- **WHEN** um usuario se cadastra com email listado como administrador
- **THEN** o usuario recebe papel ADMIN

#### Scenario: Email comum
- **WHEN** um usuario se cadastra com email que nao esta na lista
- **THEN** o usuario recebe papel USER

#### Scenario: Admin configurado nao e rebaixado pela API
- **WHEN** um ADMIN tenta mudar para USER o papel de um email listado na configuracao
- **THEN** o sistema responde 422 e o papel continua ADMIN
