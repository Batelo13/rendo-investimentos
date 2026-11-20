## 1. DAST

- [x] 1.1 `scripts/dast_zap.sh` com instância descartável e ZAP 2.16.1 fixado. Teste: execução local com `APROVADO` (0 WARN, 0 FAIL, 9 IGNORE).
- [x] 1.2 `security/zap-baseline.conf` com justificativa por regra e triagem em `security/triagem.md`. Teste: o mesmo scan.

## 2. Correções

- [x] 2.1 CSP restrita + COOP, fora do `/h2-console`. Teste: `SecurityHeadersIntegrationTest.paginaPublicaTemCabecalhosDeSeguranca` e `consoleH2NaoRecebeCspRestrita`.
- [x] 2.2 Tema em `js/tema-inicial.js`, sem script ou handler inline. Teste: `LiquidGlassMarkupTest.paginasAplicamTemaAntesDosStylesheets`.
- [x] 2.3 Formulários de cadastro e verificação em POST com CSRF. Teste: ZAP sem 10024 e 10202. Playwright: cadastro, verificação e dashboard sem violação de CSP no console.

## 3. Validação

- [x] 3.1 Suíte `mvnw test` verde.
