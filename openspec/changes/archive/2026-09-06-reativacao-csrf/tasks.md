## 1. Backend

- [x] 1.1 Habilitar CSRF em `SecurityConfig.java` (`.csrf(csrf -> csrf.ignoringRequestMatchers(...))`), ignorando só `/h2-console/**`

## 2. Frontend — token exposto ao navegador

- [x] 2.1 `login.html`: meta tags `_csrf`/`_csrf_header` no `<head>`
- [x] 2.2 `login.html`: hidden input no form nativo de login
- [x] 2.3 `dashboard.html`: meta tags `_csrf`/`_csrf_header` no `<head>`
- [x] 2.4 `dashboard.html`: hidden input no form nativo de logout

## 3. Frontend — envio do token via JavaScript

- [x] 3.1 `dashboard.js`: header CSRF no helper `api()` (cobre todo fetch do dashboard)
- [x] 3.2 `login.js`: header CSRF nos 3 fetch (cadastro, verificar-email, reenviar-código)

## 4. Testes

- [x] 4.1 Novo `CsrfTestConfig` (`MockMvcBuilderCustomizer`) aplicando `csrf()` por padrão a todo `MockMvc`
- [x] 4.2 `@Import(CsrfTestConfig.class)` em `OperacaoIntegrationTest` e `UsuarioAuthIntegrationTest`
- [x] 4.3 Novo teste `deveRejeitarRequisicaoDeEscritaSemTokenCsrf` (MockMvc isolado, sem o default, prova 403 real sem token)
- [x] 4.4 Suite completa passando (76/76)

## 5. Verificação manual

- [x] 5.1 Cadastro (POST /usuarios) via navegador real
- [x] 5.2 Verificação de email (POST /usuarios/verificar-email) via navegador real
- [x] 5.3 Login nativo (POST /login) via navegador real
- [x] 5.4 Cadastro de corretora via dashboard (POST /corretoras, fetch) via navegador real
- [x] 5.5 Logout nativo (POST /logout) via navegador real
