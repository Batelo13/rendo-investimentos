# Triagem de seguranca -- 2026-11-16

Change OpenSpec: `endurecimento-seguranca`. Metodo: `.agents/prompts/triagem-seguranca.md`.

## Camadas automaticas

| Camada | Itens verificados | Resultado |
| --- | --- | --- |
| semgrep 1.x (`p/java`, `p/secrets`, `p/owasp-top-ten`) | 167 regras, 109 arquivos | 0 achados |
| SpotBugs 4.10.4.1 + FindSecBugs 1.14.0 (SECURITY, Medium) | 101 classes | 1 achado, triado abaixo |
| Busca de segredos no historico (padroes de chave/token via `git log -p`) | historico completo | 1 placeholder local, triado abaixo |
| gitleaks / OSV-Scanner | -- | **nao executados**: Docker parado na maquina durante a mudanca |

## Achados

| Achado | Classificacao | Justificativa | Acao |
| --- | --- | --- | --- |
| `UNVALIDATED_REDIRECT` em `LoginSocialFailureHandler.java:37` | NAO EXPLORAVEL | Destino fixo `/login` via `UriComponentsBuilder.fromPath`; nome/email vem do ID token assinado pelo provedor e entram so como query codificada | Exclusao pontual em `spotbugs-exclude.xml` |
| `POSTGRES_PASSWORD = "rendo-local-test"` em `docs/superpowers/plans/2026-08-25-docker-postgresql.md` | NAO EXPLORAVEL | Placeholder de teste local documentado no plano; nenhuma instancia usa esse valor fora da maquina de desenvolvimento | Nenhuma |
| IDOR em `GET /usuarios/{id}` (revisao manual) | VERDADEIRO, alta | Usuario autenticado lia CPF/email de qualquer outro | Corrigido + teste |
| Codigo de verificacao e email em log (revisao manual) | VERDADEIRO, media | Quem le o log valida conta alheia (docker: `MAIL_ENABLED=false` por padrao) | Corrigido + teste |
| Fallback do circuit breaker para qualquer excecao (revisao guiada pelo grafo) | VERDADEIRO, media | 404 externo virava 503 "circuito aberto" | Corrigido + teste |
| Senha > 72 bytes / nome sem limite (revisao manual) | VERDADEIRO, media | `IllegalArgumentException` do BCrypt virava erro interno | Corrigido + teste |
| Cabecalhos e cookie de sessao (revisao manual, ZAP do Modulo 11) | VERDADEIRO, media | Sem Referrer-Policy, Permissions-Policy, CORP, CSP; cookie sem SameSite/Secure | Corrigido + teste |
| `show-sql` no docker; PostgreSQL em `0.0.0.0` | VERDADEIRO, baixa | Configuracao de producao | Corrigido |

## Riscos aceitos

Ver `openspec/changes/endurecimento-seguranca/design.md`, secao Risks.
