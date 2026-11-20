## 1. Correção

- [x] 1.1 Liberar `/error` na cadeia de segurança. Teste: `SecurityHeadersIntegrationTest.estaticoInexistenteDevolve404`.
- [x] 1.2 Rota protegida inexistente sem sessão segue 401. Teste: `SecurityHeadersIntegrationTest.rotaProtegidaInexistenteSemSessaoDevolve401`.

## 2. Validação

- [x] 2.1 Suíte `mvnw test` verde.
