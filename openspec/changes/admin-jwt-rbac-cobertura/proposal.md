## Por quê

O professor pediu: senha forte no cadastro, um administrador que se diferencia do usuário comum e as aulas 13 (login com JWT), 14 (controle de acesso por papel) e 15 (testes e cobertura com JaCoCo). Hoje o ADMIN só existe nas regras de URL, sem nenhuma tela própria, e a API só aceita sessão com cookie.

## O que muda

- **Senha no cadastro:** mínimo de 8 caracteres, com pelo menos uma letra maiúscula e uma minúscula (400 se faltar). **BREAKING** para clientes que cadastravam senhas só com minúsculas.
- **Aula 13, JWT:** `POST /api/auth/login` (público) devolve um token HS256 assinado (`{token, tipo, expiresIn}`). `GET /api/auth/me` devolve email e papéis do token. Toda rota REST protegida passa a aceitar também `Authorization: Bearer <token>`, sem sessão. O login no navegador continua por formulário e sessão.
- **Aula 14, RBAC:** autorização por método. As regras "só ADMIN" saem da lista de URLs e vão para as próprias operações. Matriz de acesso em `design.md`.
- **Administrador:** nova tela "Administração" no dashboard, visível só para ADMIN, com o selo "Administrador" na barra lateral. Mostra os números das contas (total, ativas, bloqueadas, e-mail não verificado, administradores, novas em 30 dias), a lista de usuários com bloquear/desbloquear e a carteira de cada um (o admin age como gerente das contas).
- **Novo endpoint:** `GET /usuarios/estatisticas` (ADMIN).
- **Aula 15:** JaCoCo 0.8.15 no `verify`, com portão de cobertura de linhas e ramos no valor medido, e testes novos escritos por TDD.

## Impacto

- Endpoints públicos: `POST /api/auth/login` (novo). Novos protegidos: `GET /api/auth/me`, `GET /usuarios/estatisticas`.
- Banco: nenhuma coluna nova.
- Dependências: `spring-boot-starter-oauth2-resource-server` e o plugin `jacoco-maven-plugin` (justificadas no design).
- Configuração: `JWT_SECRET` (obrigatória fora do dev), `JWT_EXPIRATION` (padrão 60m) e `JWT_ISSUER` (padrão `rendo`).
