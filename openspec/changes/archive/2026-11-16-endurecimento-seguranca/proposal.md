## Why

Aplicacao do Modulo 11 (Seguranca) do curso desenvolvimento-assistido-IA ao Rendo. A analise
(semgrep `p/java` + `p/secrets` + `p/owasp-top-ten`: 167 regras, 109 arquivos, 0 achados; busca
de segredos no historico; revisao manual de autorizacao, erros, logs e configuracao) encontrou
problemas que as ferramentas de padrao nao pegam, porque sao de logica de autorizacao e de
configuracao:

1. **IDOR (alta):** `GET /usuarios/{id}` devolve nome, email e CPF de qualquer usuario para
   qualquer usuario autenticado -- so a listagem `GET /usuarios` exige ADMIN.
2. **Dado sensivel em log (media):** com envio de email desligado, o codigo de verificacao e o
   email do usuario vao para o log em texto claro -- inclusive no profile docker, cujo padrao e
   `MAIL_ENABLED=false`. Quem le o log valida contas alheias.
3. **Erro mascarado (media):** o fallback do circuit breaker das integracoes externas roda para
   *qualquer* excecao, nao so com o circuito aberto: CEP/CNPJ/ticker inexistente vira 503
   "indisponivel (circuit breaker aberto)" em vez de 404, e a mensagem de configuracao do token
   da brapi se perde.
4. **Entrada sem limite (media):** senha acima de 72 bytes nao pode ser codificada pelo BCrypt e
   nome sem tamanho maximo estoura a coluna -- ambos viram 500 em vez de 400.
5. **Cabecalhos e cookie (media):** faltam Referrer-Policy, Permissions-Policy,
   Cross-Origin-Resource-Policy e uma CSP minima; o cookie de sessao nao declara SameSite e nao
   tem como ser marcado Secure em producao.
6. **Configuracao de producao (baixa):** SQL logado e PostgreSQL publicado em todas as
   interfaces no compose.
7. **Processo (baixa):** nao existe portao de seguranca reproduzivel nem prompt de triagem.

## What Changes

- **BREAKING (seguranca):** `GET /usuarios/{id}` passa a responder 403 quando o usuario
  autenticado nao e o dono do registro nem ADMIN.
- Codigo de verificacao so e logado quando `app.mail.log-codigo=true` (padrao: so no profile
  dev); o email aparece mascarado no log.
- Fallback do circuit breaker so atua com o circuito aberto; demais erros chegam ao cliente com o
  status e a mensagem originais (404 para recurso externo inexistente).
- Cadastro rejeita com 400 senha acima de 72 bytes (UTF-8) e nome acima de 120 caracteres.
- Novos cabecalhos de seguranca em todas as respostas; cookie de sessao `HttpOnly` +
  `SameSite=Lax`, `Secure` configuravel por `SESSION_COOKIE_SECURE`.
- Profile docker: `show-sql=false`; compose publica o PostgreSQL so em `127.0.0.1`.
- Portao de seguranca `scripts/security_gate.sh` (semgrep, gitleaks, SpotBugs+FindSecBugs,
  SBOM CycloneDX + OSV-Scanner), prompt de triagem versionado e `AGENTS.md`.

## Capabilities

### New Capabilities
- `seguranca-aplicacao`: controle de acesso a dados de usuario por propriedade, limites de
  entrada no cadastro, preservacao do status de erro das integracoes externas, ausencia de
  codigos de verificacao em log fora de dev, e cabecalhos/cookie de sessao seguros.

### Modified Capabilities
(nenhuma -- os requisitos novos nao alteram cenarios existentes)

## Impact

- Endpoints: `GET /usuarios/{id}` (403 para nao-dono); `GET /corretoras/cnpj/{cnpj}`,
  `GET /acoes/ticker/{ticker}`, `POST /acoes`, `POST /corretoras` (404 em vez de 503 para
  recurso externo inexistente); `POST /usuarios` (400 para senha/nome acima do limite).
- Banco: nenhum impacto de esquema.
- Variaveis novas (documentadas em `.env.example`): `SESSION_COOKIE_SECURE`, `MAIL_LOG_CODE`.
- Dependencias: Spring Boot 4.1.0 -> 4.1.2 (corrige 20 CVEs transitivas); nenhuma dependencia de
  runtime nova; SpotBugs/FindSecBugs entram so como plugin
  de build executado sob demanda.
