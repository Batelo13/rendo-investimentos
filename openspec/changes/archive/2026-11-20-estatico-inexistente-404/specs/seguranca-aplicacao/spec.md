## ADDED Requirements

### Requirement: Status correto para recurso inexistente
O sistema SHALL responder 404 a um pedido sem sessão de arquivo estático público inexistente, e SHALL continuar respondendo 401 a um pedido sem sessão de qualquer rota protegida, exista ela ou não.

#### Scenario: Arquivo estatico publico inexistente
- **WHEN** um visitante sem sessão pede `GET /css/nao-existe.css`
- **THEN** o sistema responde 404

#### Scenario: Rota protegida inexistente continua exigindo login
- **WHEN** um visitante sem sessão pede uma rota protegida que não existe
- **THEN** o sistema responde 401, sem revelar se a rota existe
