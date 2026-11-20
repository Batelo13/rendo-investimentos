# Triagem de seguranca -- 2026-11-16

Change OpenSpec: `endurecimento-seguranca`. Metodo: `.agents/prompts/triagem-seguranca.md`.

## Camadas automaticas

| Camada | Itens verificados | Resultado |
| --- | --- | --- |
| semgrep 1.x (`p/java`, `p/secrets`, `p/owasp-top-ten`) | 167 regras, 109 arquivos | 0 achados |
| SpotBugs 4.10.4.1 + FindSecBugs 1.14.0 (SECURITY, Medium) | 101 classes | 1 achado, triado abaixo |
| Busca de segredos no historico (padroes de chave/token via `git log -p`) | historico completo | 1 placeholder local, triado abaixo |
| gitleaks 8.30.1 (`git` + `dir src`) | 160 commits + `src/` | 0 vazamentos |
| SBOM CycloneDX 2.9.1 + OSV-Scanner 2.6.0, Spring Boot 4.1.0 | 131 pacotes | **20 vulnerabilidades em 6 pacotes (7 criticas, 7 altas, 6 medias)** |
| SBOM CycloneDX 2.9.1 + OSV-Scanner 2.6.0, Spring Boot 4.1.2 | 131 pacotes | 0 (`No issues found`) |

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
| CVEs em `tomcat-embed-core` 11.0.22, `spring-webmvc` 7.0.8, `jackson-core`/`jackson-databind` 3.1.4, `postgresql` 42.7.11, `log4j-api` 2.25.4 (OSV) | VERDADEIRO, critica | Todas transitivas, versoes vindas do parent Spring Boot 4.1.0 | Spring Boot 4.1.0 -> 4.1.2 (patch); nenhuma sobrescrita de versao necessaria; 107 testes verdes |

## Riscos aceitos

Ver `openspec/changes/endurecimento-seguranca/design.md`, secao Risks.

# Triagem DAST -- 2026-11-20

Change OpenSpec: `dast-zap-baseline`. Comando: `bash scripts/dast_zap.sh` (ZAP 2.16.1 baseline, passivo, instancia descartavel).

| Achado ZAP | Classificacao | Justificativa | Acao |
| --- | --- | --- | --- |
| 10055 CSP sem `default-src`/`script-src`/`form-action` | VERDADEIRO, media | XSS nao era contido pela CSP | CSP restrita + teste; resta `style-src 'unsafe-inline'` e `img-src https:` (IGNORE justificado) |
| 10024 Dados sensiveis na URL (`/login?...senha=...`) | VERDADEIRO, media | Formularios de cadastro/verificacao sem `method` = GET quando o JS nao roda | `method="post"` + `th:action` |
| 10202 Formulario sem token CSRF | VERDADEIRO, baixa | Surgiu com o POST acima; o JS ja mandava o token no cabecalho | `th:action` injeta o token |
| 90004 Isolamento contra Spectre | PARCIAL | COEP `require-corp` bloquearia Google Fonts e logos externos | `Cross-Origin-Opener-Policy: same-origin`; COEP aceito como risco |
| 90003 SRI ausente | NAO EXPLORAVEL na pratica | CSS do Google Fonts varia por user-agent; hash fixo impossivel | IGNORE justificado |
| 10116 ZAP desatualizado | NAO E VULNERABILIDADE | Versao fixada de proposito | IGNORE |
| 10027, 10049, 10109, 10111, 10112 | INFORMATIVO | Ver `security/zap-baseline.conf` | IGNORE |
