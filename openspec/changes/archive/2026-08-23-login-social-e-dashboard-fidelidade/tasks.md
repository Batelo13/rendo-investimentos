## 1. OAuth2 backend — estrutura condicional

- [x] 1.1 `spring-boot-starter-oauth2-client` adicionado ao `pom.xml`
- [x] 1.2 `OAuth2ClientRegistrations` — bean condicional (`@ConditionalOnExpression`), registra só provedores com client-id+secret configurados
- [x] 1.3 Confirmado empiricamente que app sobe sem nenhuma credencial, com uma, e com as três (testado via `mvnw spring-boot:run` + curl)
- [x] 1.4 `RendoOidcUserService` — autentica só contas Rendo existentes por email, `nameAttributeKey="email"`
- [x] 1.5 `ContaOAuthNaoEncontradaException` + `LoginSocialFailureHandler` — redireciona pra cadastro pré-preenchido em vez de criar conta ou só mostrar erro genérico
- [x] 1.6 `AppleAuthorizationRequestResolver` — `response_mode=form_post` só pra Apple
- [x] 1.7 `PaginaController` expõe `provedoresSociais` pro template (só mostra botão de provedor realmente configurado)
- [x] 1.8 `application.properties` documenta as variáveis de ambiente (todas opcionais)

## 2. Bug de segurança

- [x] 2.1 Diagnosticado: `formLogin().permitAll()` só libera `/login` com querystring exata (`?error`/`?logout`)
- [x] 2.2 Corrigido com `.requestMatchers(HttpMethod.GET, "/login").permitAll()` explícito
- [x] 2.3 Verificado via curl (200 em `/login`, `/login?criarConta=1&...`, `/login?qualquer=coisa`)

## 3. Login social — frontend

- [x] 3.1 Botões Google/Apple/Microsoft em `login.html` (login e cadastro), condicionados a `provedoresSociais`
- [x] 3.2 CSS dos botões/divisor/aviso de segurança em `login.css`
- [x] 3.3 `login.js` lê `?criarConta=1&nome=...&email=...` e pré-preenche o cadastro
- [x] 3.4 Testado end-to-end no navegador: sem credenciais (seção some), com as três fake (botões aparecem, redirect real confirmado via curl pro Google), fluxo completo social→cadastro→login

## 4. Dashboard — IBOV e refinamentos

- [x] 4.1 `IbovespaClient` + `IndiceMercado` + `MercadoController` (`GET /mercado/ibovespa`)
- [x] 4.2 Testado contra a API real da brapi (token já configurado no `.env` do projeto) — 171.031,73 pts, +1,85%
- [x] 4.3 Widget IBOV na sidebar, oculto se a chamada falhar
- [x] 4.4 Ícones coloridos por categoria nos stat-cards + ícone de info decorativo
- [x] 4.5 Coluna "Preço atual" + menu (⋮ → Vender) na lista de posições da Visão Geral, reaproveitando `abrirVendaModal`
- [x] 4.6 Sparkline no card "Lucro/Prejuízo não realizado" (reaproveita `state.rendimento`)
- [x] 4.7 Linha-guia tracejada no hover do gráfico
- [x] 4.8 Aviso "Os dados podem ter até 15 minutos de atraso." na Visão Geral e na página completa de posições
- [x] 4.9 Testado end-to-end: corretora + ação + compra reais, menu de venda abrindo a posição correta, responsivo em 400px via iframe

## 5. Verificação final

- [x] 5.1 `mvnw compile` sem erros
- [x] 5.2 `mvnw test` — 55 testes, 0 falhas, 0 erros (inclui `UsuarioAuthIntegrationTest`, que cobre o fluxo de `/login`)
- [x] 5.3 Console do navegador sem erros da aplicação
