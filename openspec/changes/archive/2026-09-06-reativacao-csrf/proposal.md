## Why

A proteção CSRF do Spring Security estava desabilitada desde o início do projeto (`SecurityConfig.java`: `.csrf(csrf -> csrf.disable())`), com um comentário dizendo "reativar na fase de frontend". Essa fase chegou: hoje existem formulários Thymeleaf nativos (login, logout) e chamadas `fetch()` (cadastro, verificação de email, dashboard) que podem carregar e enviar o token — não há mais motivo técnico para deixar a proteção desligada em nenhuma rota de escrita da aplicação.

## What Changes

- **BREAKING** (somente para clientes não-navegador da API): toda rota POST/PATCH/DELETE passa a exigir um token CSRF válido, exceto `/h2-console/**` (que tem forms próprios sem suporte a esse token). Um cliente que chamava a API diretamente (curl/Postman) sem passar pelo fluxo de sessão do navegador agora recebe 403 Forbidden sem o token.
- `SecurityConfig.java`: remove o `.disable()`, habilita CSRF com `ignoringRequestMatchers` cobrindo só `/h2-console/**`.
- `login.html`/`dashboard.html`: expõem o token via `<meta name="_csrf">`/`<meta name="_csrf_header">` no `<head>`, e via `<input type="hidden">` nos dois forms nativos (login, logout).
- `dashboard.js`: o helper `api()` (ponto único de entrada de todo fetch do dashboard) manda o token como header em toda requisição.
- `login.js`: os 3 fetch (cadastro, verificar-email, reenviar-código) mandam o token como header.
- Testes: novo `CsrfTestConfig` (`MockMvcBuilderCustomizer`) aplica `csrf()` por padrão a todo `MockMvc` de `OperacaoIntegrationTest`/`UsuarioAuthIntegrationTest`, em vez de editar as 44 chamadas existentes uma por uma. Um teste novo (`deveRejeitarRequisicaoDeEscritaSemTokenCsrf`) prova, com uma instância `MockMvc` isolada sem esse default, que uma requisição de escrita sem token toma 403 de verdade.

## Capabilities

### New Capabilities
- `protecao-csrf`: exigência de token CSRF válido em toda requisição de escrita (POST/PATCH/DELETE), como é entregue ao navegador (meta tag + hidden input) e a exceção do H2 console.

### Modified Capabilities
(nenhuma — nenhuma capability existente tinha um requisito específico sobre CSRF até agora)

## Impact

- `src/main/java/.../security/SecurityConfig.java`
- `src/main/resources/templates/login.html`, `dashboard.html`
- `src/main/resources/static/js/login.js`, `dashboard.js`
- `src/test/java/.../util/CsrfTestConfig.java` (novo)
- `src/test/java/.../OperacaoIntegrationTest.java`, `UsuarioAuthIntegrationTest.java`
- Qualquer cliente externo da API REST que não seja o próprio frontend Thymeleaf (ex.: uma futura collection Postman) precisa passar a obter e enviar o token CSRF.
