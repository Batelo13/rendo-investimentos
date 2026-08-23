## Context

`spring-boot-starter-oauth2-client` + `@EnableWebSecurity`, só por estarem no classpath, fazem o Spring Security exigir um bean `ClientRegistrationRepository` de verdade no contexto (não um bean que existe mas resolve pra `null`) — descoberto empiricamente: um `@Bean` retornando `null` quando nenhum provedor está configurado quebra o boot da aplicação, porque `OAuth2ClientConfiguration$OAuth2ClientWebMvcSecurityConfiguration` (Spring Security, não Boot) tem uma dependência não-opcional desse tipo assim que o starter está no classpath.

## Goals / Non-Goals

**Goals:**
- Login social real (Google/Microsoft/Apple), condicionado a credenciais, sem quebrar o boot sem elas.
- Nenhuma conta criada sem CPF.
- IBOV real na sidebar, sem dado inventado.

**Non-Goals:**
- Não implementar geração/rotação automática do JWT de client-secret da Apple (isso é feito uma vez fora do runtime, com a chave `.p8` baixada do Apple Developer).
- Não criar conta automaticamente a partir de login social (CPF continua exigido).
- Não buscar sparkline intraday do IBOV (recurso pago na API usada).

## Decisions

- **`@ConditionalOnExpression` no `@Bean`, não `Optional<>` no ponto de injeção**: a definição do bean só é registrada quando pelo menos um client-id está configurado (`!('${oauth2.google.client-id:}'+'${oauth2.microsoft.client-id:}'+'${oauth2.apple.client-id:}').isBlank()`). Isso faz o restante da configuração de OAuth2 do Spring Security enxergar "nenhum candidato" (não "um candidato nulo"), que é o único jeito de fazer `OAuth2ClientWebMvcSecurityConfiguration` ser pulado de verdade. Alternativa tentada e descartada: bean retornando `null` quando a lista está vazia — compila, mas quebra o boot com `UnsatisfiedDependencyException`.
- **Todos os três provedores como OIDC (`openid` no scope)**: a Apple não tem endpoint REST de userinfo, só devolve um `id_token` — então ela OBRIGATORIAMENTE precisa ser OIDC. Configurar Google e Microsoft do mesmo jeito (em vez de OAuth2 "puro" com `userInfoUri`) evita ter dois caminhos de código (`OidcUserService` e `OAuth2UserService`) fazendo a mesma coisa.
- **`RendoOidcUserService` customizado**: em vez de deixar o Spring Security autenticar com o `sub` do provedor como nome do principal, o service busca o `Usuario` pelo email e devolve um `DefaultOidcUser` com `nameAttributeKey="email"` — assim `Principal.getName()` (usado em toda a API, ex. `CarteiraController`) continua devolvendo o email, sem precisar mudar nenhum controller/service existente.
- **Apple precisa de `response_mode=form_post`**: sempre que o scope pede mais que `openid` (aqui pede `name`/`email` também), a Apple exige esse parâmetro extra na URL de autorização — resolvido com um `OAuth2AuthorizationRequestResolver` customizado que só adiciona esse parâmetro pra Apple, sem mexer nos outros dois provedores.
- **Sem auto-provisionamento de conta**: login social com email sem conta Rendo lança `ContaOAuthNaoEncontradaException` (carrega nome/email), capturada por um `AuthenticationFailureHandler` que redireciona pra `/login?criarConta=1&nome=...&email=...` — o cadastro pré-preenche os campos, mas CPF e senha continuam sendo digitados pelo usuário; nenhuma linha é inserida em `usuarios` nesse fluxo.
- **IBOV via brapi (`^BVSP`), endpoint dedicado**: em vez de estender `CotacaoProvider`/`DadosCotacaoResponse` (contrato usado pelo fluxo real de compra/venda), um cliente HTTP separado (`IbovespaClient`) e um DTO próprio (`IndiceMercado`, com `variacaoPercentual`, campo que o contrato de ações não tem) mantêm o fluxo de trading intocado.

## Risks / Trade-offs

- [Bug de segurança encontrado durante o desenvolvimento: `/login?<qualquer-coisa>` caía em 401] → Corrigido com uma regra explícita `permitAll()` pra `GET /login` (path puro, sem depender da query string exata que o `formLogin()` gera sozinho). Coberto pela suíte de testes existente (`UsuarioAuthIntegrationTest`, 14 testes, todos passando).
- [Client-secret da Apple expira em até 6 meses (é um JWT assinado, não uma string fixa)] → Documentado em `application.properties` e no relatório final; fora do escopo runtime da aplicação.
- [brapi free tier não tem histórico intraday do índice] → Sem sparkline no widget IBOV, documentado no HTML/CSS como decisão deliberada, não esquecimento.
