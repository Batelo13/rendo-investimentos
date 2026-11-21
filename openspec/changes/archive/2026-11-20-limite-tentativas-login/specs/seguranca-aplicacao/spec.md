## ADDED Requirements

### Requirement: Limite de tentativas de login
O login por formulário SHALL bloquear por 15 minutos um par IP+email depois de 5 falhas seguidas, e um IP depois de 20 falhas em quaisquer emails. Durante o bloqueio, a senha SHALL NOT ser verificada, e o sistema SHALL redirecionar (302) para `/login?erro=muitas-tentativas`. Um login bem-sucedido SHALL zerar as falhas do par.

#### Scenario: Bloqueio apos 5 falhas seguidas
- **WHEN** o mesmo IP erra a senha de um email 5 vezes e tenta de novo com a senha certa
- **THEN** o sistema responde 302 para `/login?erro=muitas-tentativas`, e a tela de login mostra "Muitas tentativas de login"

#### Scenario: Bloqueio nao afeta o dono da conta em outro IP
- **WHEN** um IP bloqueado para um email existe e o dono entra de outro IP com a senha certa
- **THEN** o sistema autentica e redireciona para `/dashboard`

#### Scenario: Login certo zera as falhas
- **WHEN** o usuário erra 4 vezes, acerta e depois erra mais 4 vezes
- **THEN** o login certo seguinte é aceito

#### Scenario: Muitas falhas de um IP em emails diferentes
- **WHEN** um IP acumula 20 falhas em emails diferentes
- **THEN** qualquer login desse IP responde 302 para `/login?erro=muitas-tentativas` até a janela expirar

#### Scenario: Bloqueio expira depois da janela
- **WHEN** passam 15 minutos desde a primeira falha da janela
- **THEN** o par volta a poder tentar
