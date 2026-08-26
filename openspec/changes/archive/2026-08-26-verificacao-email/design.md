## Context

Ver `proposal.md` para o porquê. Pontos do estado atual que moldam a abordagem:

- Login é sessão via Spring Security `formLogin` (não JWT); `UsuarioDetailsService.loadUserByUsername` já usa `UserDetails.disabled = !ativo`.
- Login social (`RendoOidcUserService`) nunca cria conta — só autentica uma conta já existente pelo email, e já lança `OAuth2AuthenticationException` manualmente quando a conta está bloqueada (não passa por `UsuarioDetailsService`).
- Falhas de login social já são diferenciadas por tipo de exceção em `LoginSocialFailureHandler` (`ContaOAuthNaoEncontradaException` → redirect com parâmetros; qualquer outra → `/login?error`). Não existe hoje um `AuthenticationFailureHandler` equivalente para o `formLogin`.
- Sem Flyway/Liquibase: schema via `spring.jpa.hibernate.ddl-auto` (`create-drop` em dev/H2, `update` em docker/Postgres).
- `PasswordEncoder` (BCrypt) já é um bean disponível via injeção.
- `POST /usuarios` hoje devolve `UsuarioResponseDTO` (201 + `Location`); testado em `UsuarioAuthIntegrationTest`.
- Página de login/cadastro é um único `login.html` com JS vanilla (`login.js`) fazendo `fetch('/usuarios', ...)` para o cadastro; o restante é form nativo.

## Goals / Non-Goals

**Goals:**
- Bloquear acesso de contas com email não comprovado, sem tocar no fluxo de login social existente para contas já verificadas.
- Reaproveitar os padrões já existentes no projeto (exceções de negócio, `RestControllerAdvice`, `AuthenticationFailureHandler` por tipo de exceção, `PasswordEncoder`) em vez de introduzir mecanismos novos.
- Manter o schema gerenciado por `ddl-auto`, sem introduzir Flyway/Liquibase.

**Non-Goals:**
- Não implementar recuperação de senha (o design deixa o `TipoCodigo` genérico o suficiente para isso ser adicionado depois, mas não implementa agora).
- Não trocar o mecanismo de autenticação de sessão por JWT.
- Não redesenhar visualmente a tela de login além do necessário para a etapa de confirmação de código.

## Decisions

### Campo `emailVerified` na entidade `Usuario`
`Boolean emailVerified`, `@Column(nullable = false)`, sem valor default a nível de coluna — o `UsuarioService.cadastrar` sempre define explicitamente `false`, do mesmo jeito que já faz para `role` e `ativo`. Contas pré-existentes (antes desta mudança) precisam ser tratadas: como não há migration tool, uma conta com `emailVerified` nulo no Postgres (linhas antigas, `ddl-auto=update` adiciona a coluna sem backfill) é tratada como **verificada** pelo código (`Boolean.FALSE.equals(...)` só bloqueia quando o valor é explicitamente `false`; nulo passa) — evita trancar usuários já existentes fora do sistema no dia do deploy. Novas contas sempre recebem `false` explícito no `cadastrar`, então nunca ficam nulas.

### Nova entidade `EmailVerificationCode`
Tabela própria (`@ManyToOne` para `Usuario`), campos: `id`, `usuario`, `codigoHash`, `expiraEm`, `criadoEm`, `usado` (boolean), `tentativas` (int). Um `enum TipoCodigo { VERIFICACAO_EMAIL }` no campo `tipo` deixa a estrutura reaproveitável para um futuro fluxo de reset de senha sem criar uma segunda tabela agora — YAGNI além disso.

Hash do código: reaproveita o `PasswordEncoder` (BCrypt) já injetado no `UsuarioService`, aplicado ao código de 6 dígitos exatamente como já é aplicado à senha. Evita depender de uma segunda estratégia de hashing só para isto.

Geração: `SecureRandom.nextInt(1_000_000)` formatado com zero à esquerda (`String.format("%06d", ...)`) — 6 dígitos sempre, incluindo códigos com zeros à esquerda.

### Concorrência / uso único
A confirmação de código é `@Transactional` e busca o código ativo (`usado = false`, o mais recente por conta) dentro da transação; a checagem de expiração/tentativas e o `usado = true` acontecem na mesma transação antes do commit. Isso é suficiente para o volume e o perfil de uso deste projeto acadêmico — não há necessidade de lock explícito (`SELECT ... FOR UPDATE`) ou coluna de versão otimista adicional; duas requisições simultâneas com o mesmo código ainda podem, em tese, ler o mesmo estado "não usado" antes do commit da primeira. Ponto aceito conscientemente (ver Risks).

### Bloqueio de login (formulário) por email não verificado
Reaproveita o mecanismo `UserDetails.disabled` já usado para `ativo`: `UsuarioDetailsService` passa a computar `disabled = !ativo || emailNaoVerificado`. Para diferenciar o motivo no `formLogin` (hoje sem failure handler customizado, ao contrário do `oauth2Login`), duas exceções lançadas dentro de `UsuarioDetailsService`/`DaoAuthenticationProvider`:
- Conta com `ativo = false` → `DisabledException` padrão (comportamento atual, inalterado).
- Conta com `emailVerified = false` → nova `EmailNaoVerificadoException extends DisabledException`, checada primeiro (mensagem mais específica).

Um novo `AuthenticationFailureHandler` (mesmo padrão de `LoginSocialFailureHandler`) direciona para `/login?erro=email-nao-verificado` quando a exceção é `EmailNaoVerificadoException`, e delega para o comportamento padrão (`/login?error`) nos demais casos — sem alterar o `header().string("Location", ...endsWith("/login?error"))` já testado para bloqueio por `ativo`.

### Login social e verificação
`RendoOidcUserService.loadUser`, após confirmar que a conta existe e está ativa, marca `usuario.setEmailVerified(true)` e salva **se ainda não estava true** (evita um `UPDATE` desnecessário a cada login). Nenhuma mudança na checagem de `ativo` já existente.

### Resposta do cadastro
`POST /usuarios` passa a devolver um novo record simples (`CadastroResponseDTO(String message, boolean emailVerificationRequired)`) com `202 Accepted` em vez de `201 Created` (não há mais um recurso "pronto" para apontar via `Location`) — sinaliza claramente ao cliente que um passo adicional é necessário.

### Endpoints novos
Seguem o padrão do projeto (sem prefixo `/api`, verbos em português, recurso em `UsuarioController`):
- `POST /usuarios/verificar-email` — body `{email, codigo}`.
- `POST /usuarios/reenviar-codigo` — body `{email}`.

Ambos públicos (`permitAll` no `SecurityConfig`, mesma linha de `POST /usuarios`), já que o usuário ainda não tem sessão nesse ponto do fluxo.

### Rate limit de `/verificar-email` e `/reenviar-codigo`
Sem biblioteca nova: o próprio cooldown de 60s (coluna `criadoEm` do código mais recente) já limita `reenviar-codigo` por conta. Para `verificar-email`, o limite de 5 tentativas por código já limita tentativas de força bruta por código ativo. Não há um limitador global por IP nesta iteração (fora do escopo pedido — "não adicionar biblioteca pesada sem necessidade").

### Envio de email
`spring-boot-starter-mail` (nova dependência). Novo `EmailService` com um método `enviarCodigoVerificacao(destinatario, nome, codigo)`. Um template HTML simples (Thymeleaf, já disponível no projeto) para o corpo do email, renderizado via `TemplateEngine` já usado pelo projeto para as páginas.

Flag `app.mail.enabled` (property, lida de `MAIL_ENABLED`, default `true`) controla se o `EmailService` efetivamente despacha via `JavaMailSender` ou apenas loga (nível INFO) que enviaria — sem nunca devolver o código na resposta HTTP, em nenhum profile.

### Configuração / Docker
`spring.mail.*` entra em `application-docker.properties` (perfil onde o envio real importa) usando variáveis de ambiente (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`), seguindo exatamente o padrão já usado para `brapi.api.token`/`GOOGLE_CLIENT_ID` etc. (`${VAR:default}`). `compose.yaml` ganha as mesmas variáveis em `environment:` do serviço `aplicacao`. `.env.example` ganha as chaves sem valores reais. `MAIL_ENABLED` fica disponível em todos os perfis (default `true`; pode ser setado `false` em `.env` para dev local sem SMTP real).

## Risks / Trade-offs

- [Corrida entre duas confirmações simultâneas do mesmo código] → aceito como risco de baixa probabilidade/baixo impacto neste projeto acadêmico; mitigação futura seria `SELECT ... FOR UPDATE` ou coluna de versão otimista, não implementada agora.
- [`ddl-auto=update` não faz backfill de `emailVerified` em linhas existentes] → mitigado tratando `null` como verificado nas checagens de login, evitando trancar contas pré-existentes.
- [Mudança de contrato de `POST /usuarios` (201→202, corpo diferente)] → **BREAKING**, documentado na proposal; `login.js` e `UsuarioAuthIntegrationTest` são atualizados como parte desta mudança.
- [Sem rate limit por IP em `/reenviar-codigo`/`/verificar-email`] → mitigado parcialmente pelo cooldown de 60s e limite de 5 tentativas; aceito como suficiente para o escopo pedido.

## Migration Plan

Deploy único (sem coordenação especial): a nova coluna/tabela é criada automaticamente pelo `ddl-auto=update` no boot contra o Postgres do `compose.yaml`. Nenhum dado existente precisa de transformação — contas antigas continuam logando normalmente (tratamento de `null` acima). Rollback é reverter o deploy; a coluna nova fica órfã no schema (sem impacto, mesmo padrão já aceito pelo projeto para evoluções incrementais via `ddl-auto`).
