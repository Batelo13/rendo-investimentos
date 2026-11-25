## ADDED Requirements

### Requirement: Senha forte no cadastro
O cadastro SHALL exigir senha com no mínimo 8 caracteres, pelo menos uma letra maiúscula e pelo menos uma minúscula. Fora disso, responde 400 com a mensagem de validação da senha e nenhum usuário é criado.

#### Scenario: Senha sem maiúscula
- **WHEN** o cadastro envia a senha "senha1234"
- **THEN** o sistema responde 400 com a mensagem "Senha deve ter letra maiuscula e minuscula" e nenhum usuário é criado

#### Scenario: Senha com 7 caracteres
- **WHEN** o cadastro envia a senha "Senha12"
- **THEN** o sistema responde 400 com a mensagem de tamanho mínimo da senha

#### Scenario: Senha válida no limite
- **WHEN** o cadastro envia a senha "Senha123" (8 caracteres, maiúscula e minúscula)
- **THEN** a conta é criada (202)

### Requirement: Login por token JWT
O sistema SHALL emitir em `POST /api/auth/login` (público, JSON `{email, senha}`) um token assinado com validade configurável e responder `{token, tipo: "Bearer", expiresIn}`. Toda rota protegida SHALL aceitar `Authorization: Bearer <token>` sem criar sessão. Credencial inválida, conta bloqueada ou e-mail não verificado respondem 401 no formato de erro JSON. Excesso de tentativas responde 429.

#### Scenario: Credenciais válidas
- **WHEN** um usuário ativo e verificado faz login com email e senha corretos
- **THEN** recebe 200 com um token, `tipo` "Bearer" e `expiresIn` em segundos

#### Scenario: Senha errada
- **WHEN** o login JWT recebe a senha errada
- **THEN** o sistema responde 401 e não emite token

#### Scenario: Rota protegida sem token
- **WHEN** uma rota protegida é chamada sem sessão e sem token
- **THEN** o sistema responde 401

#### Scenario: Token adulterado ou expirado
- **WHEN** a requisição traz um token com a assinatura alterada ou já expirado
- **THEN** o sistema responde 401

#### Scenario: Token válido
- **WHEN** a requisição traz um token válido
- **THEN** a rota responde 200, e `GET /api/auth/me` devolve o email e os papéis do token

#### Scenario: Muitas tentativas
- **WHEN** o mesmo IP erra a senha de um email 5 vezes no login JWT
- **THEN** a tentativa seguinte responde 429 sem checar a senha

### Requirement: Controle de acesso por papel
As operações administrativas (listar usuários, estatísticas, bloquear ou desbloquear, ver ou reconstruir a carteira de outro usuário, cancelar operação) SHALL exigir o papel ADMIN, com sessão ou token. Um USER autenticado recebe 403, e um visitante sem autenticação recebe 401.

#### Scenario: USER com token tenta área de admin
- **WHEN** um USER com token válido chama `GET /usuarios`
- **THEN** o sistema responde 403

#### Scenario: ADMIN com token
- **WHEN** um ADMIN com token válido chama `GET /usuarios`
- **THEN** o sistema responde 200
