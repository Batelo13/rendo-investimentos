## ADDED Requirements

### Requirement: Login social comprova a posse do email
Uma autenticação social bem-sucedida numa conta Rendo existente SHALL marcar o email dessa conta como verificado, caso ainda não estivesse, já que o provedor OAuth2/OIDC já comprovou a posse daquele endereço.

#### Scenario: Conta existente com email ainda não verificado
- **WHEN** o usuário conclui o login social com sucesso e a conta Rendo correspondente ainda não tinha o email verificado
- **THEN** a conta passa a ter o email marcado como verificado
- **AND** o usuário é autenticado normalmente, sem qualquer bloqueio relacionado à verificação de email

#### Scenario: Conta existente com email já verificado
- **WHEN** o usuário conclui o login social com sucesso e a conta já tinha o email verificado
- **THEN** o comportamento de autenticação permanece o mesmo de antes desta mudança
