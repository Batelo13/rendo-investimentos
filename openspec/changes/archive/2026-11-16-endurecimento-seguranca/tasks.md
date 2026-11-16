## 1. Contexto e planejamento (Modulos 7 e 11)

- [x] 1.1 Preencher `openspec/config.yaml` com contexto, regras por artefato e guidance (revisao do diff)
- [x] 1.2 Proposal, design com STRIDE e spec `seguranca-aplicacao` (`openspec validate endurecimento-seguranca --strict`)

## 2. Controle de acesso

- [x] 2.1 `GET /usuarios/{id}`: dono ou ADMIN, demais 403 (`UsuarioAuthIntegrationTest`: proprio 200, outro 403, admin 200)

## 3. Tratamento seguro de erros

- [x] 3.1 Fallbacks das 6 integracoes so com circuito aberto (`ViaCepClientIntegrationTest`: CEP inexistente 404)

## 4. Validacao de entrada

- [x] 4.1 Senha <= 72 bytes UTF-8 e nome <= 120 caracteres no cadastro (`UsuarioAuthIntegrationTest`: 400 nos dois casos)

## 5. Dados sensiveis em log

- [x] 5.1 Codigo de verificacao so com `app.mail.log-codigo=true`; email mascarado (`EmailServiceTest` com captura de log)

## 6. Cabecalhos, cookie e configuracao de producao

- [x] 6.1 Cabecalhos de seguranca e cookie de sessao (`SecurityHeadersIntegrationTest`)
- [x] 6.2 Profile docker sem `show-sql`; compose com PostgreSQL em `127.0.0.1`; `.env.example` documentado (`docker compose config --quiet` + revisao do diff)

## 7. Portao e triagem (Modulo 11 secoes 13, 16, 17, 18)

- [x] 7.1 SpotBugs + FindSecBugs no `pom.xml` com filtro de seguranca (`./mvnw compile spotbugs:check`)
- [x] 7.2 `scripts/security_gate.sh`, `.agents/prompts/triagem-seguranca.md` e `AGENTS.md` (execucao do portao)

## 8. Dependencias vulneraveis (Modulo 11 secao 14)

- [x] 8.1 Spring Boot 4.1.0 -> 4.1.2: OSV-Scanner de 20 CVEs para 0 com 131 pacotes verificados (`./mvnw test` 107 verdes + `scripts/security_gate.sh` APROVADO)
