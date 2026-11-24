## Decisões

1. **Uma só cadeia de segurança.** O resource server do Spring Security entra na cadeia existente. O filtro de bearer só age quando há o cabeçalho `Authorization: Bearer`, então o navegador continua com sessão e CSRF, e um cliente de API usa as mesmas rotas com token. Não há segunda cadeia nem rotas duplicadas em `/api`.
2. **CSRF fica de fora para requisições com bearer e para `POST /api/auth/login`.** O token vai num cabeçalho, que outro site não consegue mandar sem CORS (não há CORS liberado). O login devolve o token no corpo e não grava cookie.
3. **Sem sessão no fluxo JWT.** O filtro de bearer guarda a autenticação só na requisição.
4. **Login JWT reaproveita as regras do formulário:** o mesmo `UserDetailsService` (conta bloqueada, e-mail não verificado), o mesmo BCrypt e o mesmo limite de tentativas (`LimiteTentativasLogin`). Quando o limite estoura, a resposta é 429.
5. **Token:** HS256 com `JWT_SECRET` de pelo menos 32 bytes (a aplicação não sobe com menos), claims `iss`, `sub` (email), `iat`, `exp` e `authorities` (`ROLE_*`). Sem dado pessoal no token, nem nome nem CPF.
6. **Papel ADMIN continua vindo só de `ADMIN_EMAILS`.** A aula 14 sugere CRUD de usuários com troca de papel. Aqui nenhum endpoint promove ninguém (requisito "Papel ADMIN concedido por configuracao"). O admin gerencia as contas por bloqueio e desbloqueio, que é desativar em vez de apagar, como a aula recomenda.
7. **Autorização por método** (`@EnableMethodSecurity`). Cada operação de admin carrega a própria regra. A lista de URLs fica só com o que é público e com "autenticado".
8. **JaCoCo** roda no `verify`. O portão começa no valor medido, arredondado para baixo, e só sobe.

### Dependências novas
- `spring-boot-starter-oauth2-resource-server`: traz `JwtEncoder`/`JwtDecoder` (Nimbus) e o filtro de bearer. Evita uma biblioteca JWT à parte e é o caminho da aula 13.
- `jacoco-maven-plugin` 0.8.15: mede a cobertura e barra o build abaixo do portão (aula 15).

## Matriz de acesso

| Recurso | Público | USER | ADMIN |
|---|---|---|---|
| `POST /api/auth/login`, `POST /usuarios` (cadastro), verificação de e-mail | sim | sim | sim |
| `GET /api/auth/me` | não | sim | sim |
| `/carteiras/me/**`, ações, corretoras, operações próprias | não | sim | sim |
| `GET /usuarios/{id}` | não | só o próprio | sim |
| `GET /usuarios`, `GET /usuarios/estatisticas` | não | não | sim |
| `PATCH /usuarios/{id}/bloquear` e `/desbloquear` | não | não | sim |
| `GET /carteiras/{id}`, `GET /carteiras/{id}/operacoes`, `PATCH /carteiras/{id}/reconstruir` | não | não | sim |
| `PATCH /operacoes/{id}/cancelar` | não | não | sim |

Regra horizontal: um USER só vê a própria carteira (`/me`) e o próprio cadastro.

## 401 x 403

401: a requisição não provou quem é (sem sessão, token ausente, adulterado ou expirado). 403: sabemos quem é, mas o papel não permite a operação.

## Por que o JWT não leva dado sensível

O payload só é assinado, não cifrado: qualquer um que tenha o token lê o conteúdo em Base64. Por isso ele leva só email e papéis.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Token assinado com HS256 e segredo de 32+ bytes, só do ambiente. O decoder valida assinatura, `exp` e `iss`. Login com BCrypt e limite de tentativas. |
| Tampering | Token alterado falha a assinatura e dá 401. Papel e `ativo` nunca vêm do cliente. |
| Repudiation | O admin bloqueia, nunca apaga, e o registro fica. Sem trilha de auditoria; criar uma se houver mais de um admin. |
| Information disclosure | O token não leva nome nem CPF. As estatísticas são só contagens. A lista de usuários (só ADMIN) não traz senha nem CPF. |
| Denial of service | O login JWT usa o mesmo limite do formulário (429). |
| Elevation of privilege | A autorização por método barra USER com 403. Nenhum endpoint muda papel. Token emitido antes de um bloqueio vale até `exp` (60 min): limite conhecido, porque não há revogação. |
