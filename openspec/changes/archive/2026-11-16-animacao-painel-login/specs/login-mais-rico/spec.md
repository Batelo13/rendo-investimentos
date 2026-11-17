## ADDED Requirements

### Requirement: Painel lateral anima textos e números
A tela de login e cadastro SHALL fazer os textos do painel lateral entrarem em cascata, contar os números dos widgets a partir de zero até o valor exibido e desenhar as linhas dos minigráficos. Isso SHALL acontecer na primeira carga e a cada troca entre login e cadastro, e SHALL NOT acontecer quando o sistema pede movimento reduzido.

#### Scenario: Troca para cadastro
- **WHEN** o usuário clica em "Criar conta"
- **THEN** os textos do painel "Bem-vindo de volta!" entram em sequência, e o patrimônio dos widgets conta até "R$ 34.586,30"

#### Scenario: Movimento reduzido
- **WHEN** o sistema operacional pede movimento reduzido
- **THEN** os textos e números aparecem prontos, sem animação
