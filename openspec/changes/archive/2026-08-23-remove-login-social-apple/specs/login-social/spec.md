## MODIFIED Requirements

### Requirement: Login social só ativa com credenciais reais
O sistema SHALL só disponibilizar login social (Google/Microsoft) para provedores cujo client-id e client-secret estejam configurados via variável de ambiente. Sem nenhuma credencial configurada, a aplicação SHALL subir normalmente e a UI SHALL não exibir nenhum botão de login social.

#### Scenario: Nenhuma credencial configurada
- **WHEN** a aplicação inicia sem nenhuma variável de ambiente de OAuth2 configurada
- **THEN** ela sobe normalmente e a tela de login não mostra nenhum botão de login social

#### Scenario: Só um provedor configurado
- **WHEN** apenas as credenciais do Google estão configuradas
- **THEN** somente o botão do Google aparece na tela de login
