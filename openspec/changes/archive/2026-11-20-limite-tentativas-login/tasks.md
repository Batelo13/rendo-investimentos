## 1. Limite

- [x] 1.1 Contador por IP+email e por IP, janela de 15 min. Teste: `LimiteTentativasLoginTest`.
- [x] 1.2 Filtro antes do login e falha registrada no handler. Teste: `LimiteTentativasLoginIntegrationTest.bloqueiaAposCincoFalhasMesmoComSenhaCerta` e `bloqueiaIpQueTestaMuitosEmails`.
- [x] 1.3 Sucesso zera o par; outro IP não é afetado. Teste: `sucessoZeraContador` e `outroIpContinuaEntrando`.
- [x] 1.4 Mensagem "muitas-tentativas" na tela de login, separada da de email não verificado. Teste: `bloqueiaAposCincoFalhasMesmoComSenhaCerta` (GET `/login?erro=muitas-tentativas`).

## 2. Validação

- [x] 2.1 Suíte `mvnw test` verde e `scripts/dast_zap.sh` aprovado.
