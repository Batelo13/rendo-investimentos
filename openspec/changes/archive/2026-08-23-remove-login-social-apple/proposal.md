## Why

O usuário pediu explicitamente pra remover a Apple do login social: nunca houve credenciais configuradas pra ela (client-secret da Apple é um JWT assinado gerado fora do runtime, com um custo de configuração real — conta paga no Apple Developer Program), e mantê-la como um provedor "pronto mas nunca usado" é código morto sem benefício.

## What Changes

- Removido o suporte a login social via Apple: `OAuth2ClientRegistrations.apple()`, `AppleAuthorizationRequestResolver` (classe inteira, só existia pro `response_mode=form_post` que a Apple exige), o botão da Apple no template de login/cadastro, e as variáveis `APPLE_CLIENT_ID`/`APPLE_CLIENT_SECRET` em `application.properties`.
- **BREAKING** (documentação apenas, nunca esteve em uso): quem eventualmente tivesse configurado `APPLE_CLIENT_ID`/`APPLE_CLIENT_SECRET` no `.env` precisa remover essas linhas — a aplicação as ignora agora.
- Google e Microsoft continuam exatamente como estavam: login social real, condicionado a credenciais, sem criação automática de conta.

## Capabilities

### Modified Capabilities
- `login-social`: escopo reduzido de "Google, Microsoft e Apple" para "Google e Microsoft".

## Impact

- `src/main/java/.../security/oauth2/OAuth2ClientRegistrations.java`, `SecurityConfig.java`
- `src/main/java/.../security/oauth2/AppleAuthorizationRequestResolver.java` (removido)
- `src/main/resources/application.properties`, `templates/login.html`
- Nenhuma mudança em Google/Microsoft, nenhuma mudança de endpoint/DTO/regra de negócio.
