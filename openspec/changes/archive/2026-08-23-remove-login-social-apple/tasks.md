## 1. Remoção

- [x] 1.1 `OAuth2ClientRegistrations`: removidos campos/método/branch da Apple, condição do bean ajustada pra só Google+Microsoft
- [x] 1.2 `AppleAuthorizationRequestResolver.java` deletado
- [x] 1.3 `SecurityConfig`: removida a customização de `.authorizationEndpoint()` (só existia pra Apple)
- [x] 1.4 `application.properties`: removidas `oauth2.apple.*` e a documentação de `APPLE_CLIENT_ID/SECRET`
- [x] 1.5 `login.html`: removido o botão da Apple (cadastro e login)

## 2. Verificação

- [x] 2.1 `mvnw compile` sem erros
- [x] 2.2 `mvnw test` — 55 testes, 0 falhas
- [x] 2.3 App sobe e testado via curl: `/oauth2/authorization/google` e `/oauth2/authorization/microsoft` continuam redirecionando corretamente pros provedores reais; `/oauth2/authorization/apple` não funciona mais; tela de login mostra só os botões Google/Microsoft
