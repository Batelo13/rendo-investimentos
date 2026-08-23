## Why

O usuário pediu explicitamente a possibilidade de criar conta/entrar com Google, Microsoft e Apple (substituindo GitHub por Apple), e que o dashboard fique fiel a uma referência visual mais densa (IBOV, ícones coloridos, sparkline no card de resultado, eixos no gráfico, menu de ações por linha). A rodada anterior (`redesign-login-cadastro`) tinha deixado ambos de fora deliberadamente — o login social por falta de credenciais OAuth2 reais, o IBOV por ser dado de mercado externo. Esta change resolve os dois de forma real, sem funcionalidade falsa.

## What Changes

- Login social real via Spring Security OAuth2 Client (Google, Microsoft, Apple) — estrutura condicional: só ativa quando há credenciais configuradas via variável de ambiente; sem credenciais, a aplicação sobe normalmente e a seção de login social simplesmente não aparece.
- Login social só autentica contas Rendo já existentes (busca por email) — CPF continua obrigatório e não é coletado por nenhum provedor OAuth2/OIDC, então nenhuma conta é criada automaticamente. Login social com email sem conta correspondente redireciona para o cadastro com nome/email pré-preenchidos.
- Correção de um bug de segurança introduzido durante o desenvolvimento: `permitAll()` do `formLogin()` só liberava `/login` com querystring exata (`?error`, `?logout`), qualquer outra querystring (ex. `?criarConta=1`) caía em 401.
- Endpoint novo `GET /mercado/ibovespa` (dado real via brapi, mesmo provedor já usado para ações brasileiras) alimentando um widget decorativo do IBOV na sidebar do dashboard — sem sparkline, porque intervalo intraday do índice é recurso pago na API usada (plano gratuito só devolve fechamento diário).
- Dashboard: ícones coloridos por categoria nos stat-cards, ícone de info decorativo, coluna "Preço atual" e menu de três pontos (⋮ com ação "Vender") na lista de posições da Visão Geral, sparkline no card "Lucro/Prejuízo não realizado" (reaproveitando a série já carregada do gráfico principal), linha-guia tracejada + ponto no hover do gráfico, aviso "Os dados podem ter até 15 minutos de atraso." abaixo da lista de posições.

## Capabilities

### New Capabilities
- `login-social`: login/cadastro via Google, Microsoft e Apple, condicionado a credenciais reais configuradas, sem criação automática de conta.

### Modified Capabilities
- `redesign-dashboard-visual`: ícones coloridos por categoria, coluna de preço atual e menu de ações na lista de posições, sparkline no card de resultado não realizado, linha-guia no hover do gráfico, widget de índice IBOV na sidebar (dado real).

## Impact

- `pom.xml` (nova dependência `spring-boot-starter-oauth2-client`)
- `security/SecurityConfig.java`, `security/oauth2/*` (novo pacote), `controller/PaginaController.java`
- `controller/MercadoController.java`, `integration/IbovespaClient.java`, `integration/IndiceMercado.java` (novos)
- `application.properties` (variáveis de ambiente novas, todas opcionais)
- `templates/login.html`, `static/css/login.css`, `static/js/login.js`
- `templates/dashboard.html`, `static/css/dashboard.css`, `static/js/dashboard.js`
- Nenhuma entidade, DTO de cadastro/login por formulário, ou regra de validação existente foi alterada.
