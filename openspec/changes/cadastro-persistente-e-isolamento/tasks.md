## 1. Persistencia

- [ ] 1.1 Dev em H2 arquivo + `ddl-auto=update`; testes com H2 em memoria proprio (subir a app, cadastrar, reiniciar, cadastro presente)

## 2. CPF protegido

- [ ] 2.1 CPF como HMAC-SHA256 (`CPF_HASH_KEY`), fora da API e da mensagem 409 (`UsuarioAuthIntegrationTest`: resumo gravado, 409 sem numero, resposta sem cpf)
- [ ] 2.2 Conversao na inicializacao de CPFs em texto puro (`ProtecaoCpfMigracaoTest`)

## 3. Administrador por configuracao

- [ ] 3.1 `ADMIN_EMAILS` no cadastro e na inicializacao (`UsuarioAuthIntegrationTest`: email configurado ADMIN, comum USER)

## 4. Isolamento entre contas

- [ ] 4.1 Testes de isolamento de todas as rotas com id e de `/me`/`/dashboard` (`IsolamentoEntreContasIntegrationTest`)
