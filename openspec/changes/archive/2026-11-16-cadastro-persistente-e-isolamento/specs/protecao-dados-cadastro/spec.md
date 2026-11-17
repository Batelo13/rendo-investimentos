## ADDED Requirements

### Requirement: Cadastros persistem entre reinicios
O sistema SHALL manter os cadastros e dados das carteiras quando a aplicacao e reiniciada, inclusive
no ambiente de desenvolvimento.

#### Scenario: Cadastro sobrevive ao reinicio
- **WHEN** um usuario e cadastrado e a aplicacao e reiniciada
- **THEN** o usuario continua existindo e consegue entrar com as mesmas credenciais

### Requirement: CPF nao armazenado nem exposto em claro
O sistema SHALL armazenar o CPF apenas como resumo criptografico com chave secreta, SHALL NOT
devolver o CPF em nenhuma resposta da API e SHALL rejeitar CPF duplicado com 409 sem repetir o
numero na mensagem.

#### Scenario: CPF gravado como resumo
- **WHEN** um usuario se cadastra com um CPF valido
- **THEN** o valor armazenado nao contem os digitos do CPF e tem 64 caracteres hexadecimais

#### Scenario: CPF duplicado
- **WHEN** um segundo cadastro usa o CPF de um usuario existente
- **THEN** o sistema responde 409 com mensagem que nao contem o numero do CPF

#### Scenario: Resposta de usuario sem CPF
- **WHEN** um usuario consulta o proprio registro
- **THEN** a resposta nao contem o campo cpf

### Requirement: Papel ADMIN concedido por configuracao
O sistema SHALL conceder papel ADMIN somente aos emails listados na configuracao do servidor, no
cadastro e na inicializacao.

#### Scenario: Email configurado como administrador
- **WHEN** um usuario se cadastra com email listado como administrador
- **THEN** o usuario recebe papel ADMIN

#### Scenario: Email comum
- **WHEN** um usuario se cadastra com email que nao esta na lista
- **THEN** o usuario recebe papel USER

### Requirement: Isolamento de dados entre contas
O sistema SHALL mostrar a cada usuario apenas os dados da propria sessao e SHALL responder 403 a
qualquer rota que receba o id de outro usuario quando quem pede nao e ADMIN.

#### Scenario: Usuario tenta abrir dados de outra conta pelo id
- **WHEN** um usuario autenticado sem papel ADMIN acessa posicoes, operacoes, reconstrucao ou registro de outro usuario pelo id
- **THEN** o sistema responde 403 em todas as rotas

#### Scenario: Link do dashboard de outra pessoa
- **WHEN** um usuario abre o link do dashboard copiado de outra pessoa
- **THEN** ve apenas a propria carteira, e sem sessao e redirecionado ao login
