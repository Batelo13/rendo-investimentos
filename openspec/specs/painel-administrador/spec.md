# painel-administrador Specification

## Purpose
TBD - created by archiving change admin-jwt-rbac-cobertura. Update Purpose after archive.
## Requirements
### Requirement: Estatisticas de contas para o administrador
`GET /usuarios/estatisticas` SHALL devolver, só para ADMIN, o total de contas, as ativas, as bloqueadas, as com e-mail não verificado, os administradores e as contas criadas nos últimos 30 dias. Um USER recebe 403.

#### Scenario: Contagens refletem o banco
- **WHEN** há contas cadastradas, entre elas uma bloqueada e uma ADMIN, e o ADMIN chama o endpoint
- **THEN** a resposta traz `total`, `ativas`, `bloqueadas`, `emailNaoVerificado`, `administradores` e `novasUltimos30Dias` iguais às contagens do banco

#### Scenario: USER barrado
- **WHEN** um USER chama `GET /usuarios/estatisticas`
- **THEN** o sistema responde 403

### Requirement: Tela de administracao no dashboard
O dashboard SHALL mostrar a navegação "Administração" e o selo "Administrador" somente a usuários ADMIN. A tela exibe as estatísticas, a lista de usuários com ação de bloquear ou desbloquear e a consulta da carteira de cada usuário. Para USER, nada disso aparece no HTML.

#### Scenario: Admin vê a área
- **WHEN** um ADMIN abre `/dashboard`
- **THEN** o HTML traz o item de navegação "Administração" e o selo "Administrador"

#### Scenario: Usuário comum não vê
- **WHEN** um USER abre `/dashboard`
- **THEN** o HTML não traz o item "Administração" nem o selo

### Requirement: Administrador cria contas
`POST /usuarios/admin` SHALL criar, só para ADMIN, uma conta com nome, email, CPF, senha e papel. A conta nasce ativa, com e-mail verificado e com carteira. Senha fraca, CPF inválido ou campo ausente respondem 400. Email ou CPF repetidos respondem 409. A resposta (201) nunca traz senha, hash nem CPF.

#### Scenario: Admin cria conta que já pode entrar
- **WHEN** um ADMIN cria a conta "nova@example.com" com senha "Senha123" e papel USER
- **THEN** o sistema responde 201 com o papel USER e sem senha, e a conta entra com essa senha

#### Scenario: Email repetido
- **WHEN** um ADMIN cria uma conta com email já cadastrado
- **THEN** o sistema responde 409

#### Scenario: USER barrado
- **WHEN** um USER chama `POST /usuarios/admin`
- **THEN** o sistema responde 403

### Requirement: Administrador muda o papel de outra conta
`PATCH /usuarios/{id}/papel` SHALL trocar o papel entre USER e ADMIN, só para ADMIN. O sistema SHALL responder 422 quando o admin tenta mudar o próprio papel ou rebaixar o último ADMIN, e 404 quando a conta não existe.

#### Scenario: Promover usuário
- **WHEN** um ADMIN muda para ADMIN o papel de um USER
- **THEN** o sistema responde 200 com o papel ADMIN

#### Scenario: Próprio papel
- **WHEN** um ADMIN tenta mudar o próprio papel
- **THEN** o sistema responde 422 e o papel não muda

### Requirement: Administrador redefine senha
`PUT /usuarios/{id}/senha` SHALL trocar a senha de uma conta, só para ADMIN, seguindo a regra de senha forte. A resposta é 204 sem corpo. Depois disso, a senha antiga falha e a nova funciona.

#### Scenario: Senha redefinida
- **WHEN** um ADMIN redefine a senha de uma conta para "NovaSenha9"
- **THEN** o sistema responde 204, o login com a senha antiga falha e o login com a nova funciona

#### Scenario: Senha fraca recusada
- **WHEN** um ADMIN tenta redefinir a senha para "fraca123"
- **THEN** o sistema responde 400 e a senha não muda

### Requirement: Administrador nao bloqueia a si mesmo
`PATCH /usuarios/{id}/bloquear` SHALL responder 422 quando o id é o do próprio ADMIN autenticado.

#### Scenario: Autobloqueio
- **WHEN** um ADMIN tenta bloquear a própria conta
- **THEN** o sistema responde 422 e a conta continua ativa

