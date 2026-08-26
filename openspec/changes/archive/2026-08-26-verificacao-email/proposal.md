## Why

Hoje qualquer email digitado no cadastro tradicional (`POST /usuarios`) vira conta ativa e apta a logar imediatamente, sem nenhuma prova de que o usuário realmente controla aquele endereço. Isso permite cadastro com emails inválidos ou de terceiros e deixa a plataforma sem um mecanismo básico de confiança na identidade do usuário antes de liberar acesso.

## What Changes

- Cadastro tradicional passa a criar a conta como não verificada (`emailVerified = false`) em vez de já liberar login.
- Um código numérico de 6 dígitos é gerado (SecureRandom), armazenado com hash (reaproveitando o `PasswordEncoder` já existente) e enviado por email real ao endereço informado, expirando em 10 minutos.
- Novo endpoint para confirmar o código e marcar a conta como verificada.
- Novo endpoint para reenviar o código (com cooldown de 60s e invalidação do código anterior), protegido por limite de tentativas por código (máx. 5).
- **BREAKING**: o corpo de resposta de `POST /usuarios` muda de `UsuarioResponseDTO` para um envelope indicando que a verificação é necessária (`{"message": ..., "emailVerificationRequired": true}`).
- Login por formulário (Spring Security) passa a bloquear contas com `emailVerified = false`, reaproveitando o mecanismo `UserDetails.disabled` já usado hoje para o campo `ativo`, distinguindo o motivo do bloqueio (conta desativada vs. email não verificado) do mesmo jeito que `LoginSocialFailureHandler` já distingue motivos de falha no login social.
- Login social (Google/Microsoft) não é bloqueado por email não verificado: como o provedor já garante a posse do email, uma autenticação social bem-sucedida marca a conta correspondente como verificada automaticamente.
- Novo `EmailService` (via `spring-boot-starter-mail`, dependência ainda não presente no projeto) com envio desligável por `MAIL_ENABLED=false` (útil em dev, sem expor o código OTP nas respostas da API).
- Tela de login/cadastro ganha um passo de confirmação (input do código de 6 dígitos + reenvio) reaproveitando o painel de cadastro já existente.

## Capabilities

### New Capabilities
- `verificacao-email`: geração, envio, confirmação e reenvio do código de verificação de email pós-cadastro, e bloqueio de login para contas não verificadas.

### Modified Capabilities
- `login-social`: uma autenticação social bem-sucedida numa conta existente passa a marcar o email dessa conta como verificado (o provedor OAuth já comprova a posse do email).

## Impact

- **Entidade `Usuario`**: novo campo `emailVerified` (default `false`).
- **Nova entidade** `EmailVerificationCode` (tabela nova via `ddl-auto`, sem Flyway/Liquibase no projeto).
- **`UsuarioService`**: fluxo de `cadastrar` passa a gerar/enviar código em vez de retornar a conta pronta para uso.
- **`UsuarioController`**: novos endpoints de verificação/reenvio; resposta de `POST /usuarios` muda de formato (afeta `login.js` e o teste `UsuarioAuthIntegrationTest`).
- **`UsuarioDetailsService`**: passa a considerar `emailVerified` (além de `ativo`) para `disabled`.
- **`security/oauth2`**: nova exceção + failure handler para diferenciar "conta bloqueada" de "email não verificado" no login por formulário; `RendoOidcUserService` passa a marcar `emailVerified = true` ao autenticar.
- **`pom.xml`**: nova dependência `spring-boot-starter-mail`.
- **Configuração**: novas properties `spring.mail.*` e `MAIL_ENABLED` em `application.properties`/`application-docker.properties`, novas variáveis em `compose.yaml` e `.env.example` (sem credenciais reais).
- **Frontend**: `login.html`/`login.js`/`login.css` ganham o passo de confirmação de código.
- **Testes**: `UsuarioAuthIntegrationTest` precisa de ajustes (o cadastro deixa de logar imediatamente); novos testes cobrindo o fluxo de verificação.
