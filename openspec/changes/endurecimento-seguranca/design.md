## Context

Modulo 11 do curso: "a IA propoe, ferramentas verificam, pessoas decidem". As camadas
automaticas (SAST, segredos, SCA) nao acusaram nada no Rendo; os riscos reais estao em
autorizacao, configuracao e tratamento de erro, encontrados por revisao guiada pelo grafo do
projeto (graphify: `RecursoNaoEncontradoException` liga 12 comunidades, o que expos o fallback).

## Modelagem de ameacas (STRIDE)

| Ameaca | Pergunta | No Rendo | Controle nesta mudanca |
| --- | --- | --- | --- |
| **S**poofing | Alguem se passa por outro? | Login por sessao + OIDC; codigo de verificacao em log permite validar conta alheia | Codigo fora do log fora de dev; cookie `HttpOnly`/`SameSite=Lax`/`Secure` configuravel |
| **T**ampering | Alguem altera o que nao devia? | CSRF ativo; catalogo de acoes editavel por qualquer usuario (requisito da spec `exclusao-acao-catalogo`) | Risco aceito e registrado (ver Riscos) |
| **R**epudiation | Da para negar uma acao? | Operacoes sao livro-razao imutavel com cancelamento administrativo | Sem mudanca |
| **I**nformation disclosure | Algo vaza? | IDOR em `GET /usuarios/{id}` (CPF/email); email e codigo em log; SQL em log no docker | Checagem de propriedade; log mascarado; `show-sql=false` no docker |
| **D**enial of service | Da para derrubar? | Senha > 72 bytes e nome sem limite geram 500; sem rate limit no login | Limites de entrada (400); rate limit fica como risco remanescente |
| **E**levation of privilege | Da para ganhar poder? | Regras de papel no `SecurityConfig`; endpoint de leitura de usuario sem checagem de dono | Dono ou ADMIN, demais 403 |

## Decisions

### D1. Propriedade checada no controller, 403 para nao-dono
O endpoint compara o email do principal com o do usuario buscado; ADMIN sempre pode. Responder
403 (e nao 404) mantem o comportamento do Spring Security para acesso negado e o contrato
existente de "404 = id inexistente". Alternativa rejeitada: restringir a rota so a ADMIN --
quebraria o acesso legitimo do usuario ao proprio registro, coberto por teste existente.

### D2. Fallback recebe `CallNotPermittedException`
O Resilience4j 2.2 decide o fallback so pelo tipo do parametro; `ignore-exceptions` afeta apenas a
contagem de falhas (conferido no bytecode de `DefaultFallbackDecorator`). Trocar `Throwable` por
`CallNotPermittedException` faz o fallback rodar so com circuito aberto; o resto propaga com o
status certo. Nenhuma mudanca de configuracao.

### D3. Limite de senha em bytes, validado na borda
BCrypt considera so 72 bytes e o Spring Security 7 recusa senha maior. Uma anotacao de Bean
Validation (`@TamanhoMaximoBytes`) mede UTF-8, para que "72 caracteres acentuados" tambem seja
recusado com 400 pelo mesmo caminho dos demais erros de validacao.

### D4. Log do codigo atras de propriedade explicita
`app.mail.log-codigo` (env `MAIL_LOG_CODE`), padrao `false`; o profile dev liga, preservando o
fluxo de desenvolvimento. O email no log e mascarado (`p***@example.com`).

### D5. Cabecalhos pelo DSL do Spring Security
`Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy` negando camera,
microfone e geolocalizacao, `Cross-Origin-Resource-Policy: same-origin` (achado ZAP 90004 do
Modulo 11) e CSP minima `object-src 'none'; base-uri 'self'; frame-ancestors 'self'`. CSP com
`script-src` fica fora: as paginas tem scripts inline (tema antes do primeiro paint) e
`onerror` inline no logo; restringir agora quebraria a interface.

### D6. Portao como script, nao como skill
Igual ao Modulo 11 secao 16: `scripts/security_gate.sh` roda igual para qualquer pessoa, agente ou
CI. Ferramentas com versao fixada (semgrep via `uvx`, imagens Docker `gitleaks:v8.30.1` e
`osv-scanner:v2.6.0`, plugins Maven fixados). SpotBugs com `threshold=Medium` so na categoria
SECURITY, para nao reprovar codigo correto com avisos informativos.

Dependencias novas: nenhuma de runtime. `spotbugs-maven-plugin` 4.10.4.1 + `findsecbugs-plugin`
1.14.0 (versoes usadas no Modulo 11) entram como plugin de build sem execucao ligada ao ciclo de
vida -- so rodam via `spotbugs:check` no portao.

## Risks / Trade-offs

- **Catalogo editavel por qualquer usuario** (criar/excluir acao, criar corretora): requisito
  explicito da spec atual; mudar exige decisao de produto. Aceito.
- **Sem rate limit no login/cadastro:** exige dependencia nova ou filtro proprio com estado;
  fica como proxima etapa. A verificacao de email ja limita 5 tentativas por codigo.
- **H2 console liberado no profile dev:** banco em memoria, so em desenvolvimento; desligado no
  docker. Aceito.
- **CSP sem `script-src`:** ver D5.
- **SCA nao executado nesta maquina:** OSV-Scanner e gitleaks rodam via Docker no portao; o
  Docker local estava parado durante a mudanca.
