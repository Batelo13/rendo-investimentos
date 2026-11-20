# seguranca-aplicacao Specification

## Purpose
TBD - created by archiving change endurecimento-seguranca. Update Purpose after archive.
## Requirements
### Requirement: Acesso a dados de usuario restrito ao dono ou ADMIN
O sistema SHALL devolver os dados de um usuario por id somente ao proprio usuario autenticado ou a
um usuario com papel ADMIN.

#### Scenario: Usuario consulta o proprio registro
- **WHEN** um usuario autenticado consulta o proprio registro pelo id
- **THEN** o sistema responde 200 com os dados do usuario

#### Scenario: Usuario consulta registro de outro usuario
- **WHEN** um usuario autenticado sem papel ADMIN consulta o registro de outro usuario pelo id
- **THEN** o sistema responde 403 e nao devolve nenhum dado do outro usuario

#### Scenario: Administrador consulta registro de outro usuario
- **WHEN** um usuario ADMIN consulta o registro de outro usuario pelo id
- **THEN** o sistema responde 200 com os dados do usuario consultado

### Requirement: Limites de entrada no cadastro
O sistema SHALL rejeitar o cadastro com 400 e mensagem de validacao quando a senha exceder 72 bytes
em UTF-8 ou o nome exceder 120 caracteres, sem erro interno.

#### Scenario: Senha acima de 72 bytes
- **WHEN** o cadastro envia uma senha com mais de 72 bytes em UTF-8
- **THEN** o sistema responde 400 com a mensagem de validacao da senha e nenhum usuario e criado

#### Scenario: Nome acima de 120 caracteres
- **WHEN** o cadastro envia um nome com mais de 120 caracteres
- **THEN** o sistema responde 400 com a mensagem de validacao do nome e nenhum usuario e criado

### Requirement: Erros de integracao externa preservam o status
O sistema SHALL responder 503 com a mensagem de circuito aberto somente quando o circuito da
integracao estiver aberto; um recurso inexistente na fonte externa SHALL responder 404 com a
mensagem de nao encontrado.

#### Scenario: CEP inexistente com circuito fechado
- **WHEN** a consulta de CEP recebe da fonte externa a indicacao de CEP inexistente
- **THEN** o sistema responde 404 com a mensagem de CEP nao encontrado, e nao 503

### Requirement: Codigo de verificacao fora do log
O sistema SHALL NOT registrar em log o codigo de verificacao de email, exceto quando o registro de
codigo estiver explicitamente habilitado (padrao apenas no ambiente de desenvolvimento), e SHALL
mascarar o endereco de email nas mensagens de log.

#### Scenario: Envio desligado sem registro de codigo habilitado
- **WHEN** um codigo de verificacao e gerado com envio de email desligado e registro de codigo desabilitado
- **THEN** o log nao contem o codigo nem o endereco de email completo

### Requirement: Cabecalhos de seguranca e cookie de sessao
O sistema SHALL enviar em toda resposta os cabecalhos `X-Content-Type-Options: nosniff`,
`Referrer-Policy: strict-origin-when-cross-origin`, `Cross-Origin-Resource-Policy: same-origin`,
`Permissions-Policy` negando camera, microfone e geolocalizacao, e uma `Content-Security-Policy`
com `object-src 'none'`, `base-uri 'self'` e `frame-ancestors 'self'`; o cookie de sessao SHALL
ser `HttpOnly` e `SameSite=Lax`.

#### Scenario: Pagina publica com cabecalhos de seguranca
- **WHEN** um visitante sem sessao acessa a pagina inicial
- **THEN** a resposta contem todos os cabecalhos de seguranca listados

### Requirement: Status correto para recurso inexistente
O sistema SHALL responder 404 a um pedido sem sessão de arquivo estático público inexistente, e SHALL continuar respondendo 401 a um pedido sem sessão de qualquer rota protegida, exista ela ou não.

#### Scenario: Arquivo estatico publico inexistente
- **WHEN** um visitante sem sessão pede `GET /css/nao-existe.css`
- **THEN** o sistema responde 404

#### Scenario: Rota protegida inexistente continua exigindo login
- **WHEN** um visitante sem sessão pede uma rota protegida que não existe
- **THEN** o sistema responde 401, sem revelar se a rota existe

### Requirement: Politica de conteudo restrita nas paginas
As páginas da aplicação SHALL responder com uma política de segurança de conteúdo que só permite scripts da própria origem, sem script inline nem `eval`, e que restringe o envio de formulários à própria origem. As páginas SHALL NOT conter script inline nem atributos de evento inline.

#### Scenario: Pagina publica com politica restrita
- **WHEN** um visitante pede `GET /`
- **THEN** o sistema responde 200 com uma política de conteúdo que tem `default-src 'self'`, `script-src 'self'` sem `'unsafe-inline'` e `form-action 'self'`, além de `Cross-Origin-Opener-Policy: same-origin`

#### Scenario: Tema aplicado antes do primeiro paint sem script inline
- **WHEN** qualquer página é carregada com o tema claro salvo
- **THEN** o tema é aplicado por um script da própria origem carregado antes das folhas de estilo, sem violação da política de conteúdo

### Requirement: Dados de cadastro nunca na URL
Os formulários de cadastro e de verificação de email SHALL usar o método POST com token CSRF, mesmo quando enviados sem JavaScript.

#### Scenario: Envio sem JavaScript
- **WHEN** o formulário de cadastro é enviado sem o JavaScript da página
- **THEN** nome, email, CPF e senha não aparecem na URL; a requisição é POST e o sistema responde com erro 4xx sem criar conta

### Requirement: DAST reproduzivel
O projeto SHALL ter um comando de DAST que executa o ZAP baseline contra uma instância descartável e SHALL reprovar (código de saída 1) se surgir um alerta não triado ou se o scan não puder rodar.

#### Scenario: Scan sem alertas novos
- **WHEN** o comando de DAST roda com Docker disponível
- **THEN** ele termina com 0 e registra relatório HTML e JSON, com cada alerta aceito justificado no arquivo de regras

