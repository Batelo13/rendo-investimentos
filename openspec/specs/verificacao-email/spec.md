# verificacao-email Specification

## Purpose

Garante que uma conta criada pelo cadastro tradicional só ganha acesso à plataforma depois de comprovar a posse do email informado, através de um código numérico de curta duração enviado por email.

## Requirements

### Requirement: Cadastro tradicional cria conta não verificada
Toda conta criada por `POST /usuarios` SHALL nascer com o email marcado como não verificado, e SHALL NOT conceder acesso imediato ao sistema.

#### Scenario: Cadastro bem-sucedido
- **WHEN** um usuário completa o cadastro tradicional com dados válidos
- **THEN** a conta é criada com o email não verificado
- **AND** a resposta indica que a verificação do email é necessária, sem devolver o código gerado

### Requirement: Código de verificação de 6 dígitos é gerado e enviado por email
Ao criar a conta (ou ao reenviar), o sistema SHALL gerar um código numérico de exatamente 6 dígitos usando um gerador aleatório seguro, SHALL armazenar apenas uma forma protegida do código (nunca o valor em texto puro) e SHALL enviá-lo por email para o endereço informado no cadastro.

#### Scenario: Geração após cadastro
- **WHEN** uma conta é criada pelo cadastro tradicional
- **THEN** um código numérico de 6 dígitos é gerado e um email contendo esse código é enviado ao endereço cadastrado
- **AND** o valor armazenado no banco não permite recuperar o código original diretamente

### Requirement: Código expira em 10 minutos
Um código de verificação SHALL deixar de ser aceito 10 minutos após sua geração.

#### Scenario: Tentativa de uso após expiração
- **WHEN** o usuário envia um código que foi gerado há mais de 10 minutos
- **THEN** o sistema rejeita a confirmação informando que o código expirou, sem verificar a conta

### Requirement: Confirmação de código verifica a conta
O sistema SHALL expor uma operação que recebe o email e o código informados pelo usuário, e que marca a conta como verificada somente quando o código corresponde ao código ativo daquela conta, ainda não expirado, ainda não utilizado e dentro do limite de tentativas.

#### Scenario: Código correto e válido
- **WHEN** o usuário informa o código correto antes de expirar e dentro do limite de tentativas
- **THEN** a conta passa a ter o email verificado
- **AND** aquele código não pode mais ser usado novamente

#### Scenario: Código incorreto
- **WHEN** o usuário informa um código que não corresponde ao código ativo da conta
- **THEN** a conta continua não verificada
- **AND** a tentativa é contabilizada
- **AND** o sistema não revela detalhes internos sobre o motivo da rejeição

#### Scenario: Conta já verificada
- **WHEN** o usuário tenta confirmar um código para uma conta cujo email já está verificado
- **THEN** o sistema informa que a conta já está verificada, sem processar o código

### Requirement: Limite de tentativas por código
Cada código SHALL aceitar no máximo 5 tentativas de confirmação incorretas. Ao atingir o limite, aquele código SHALL deixar de ser aceito mesmo que o valor correto seja informado depois.

#### Scenario: Sexta tentativa após 5 erros
- **WHEN** o usuário já errou o código 5 vezes e tenta novamente, mesmo com o valor correto
- **THEN** o sistema rejeita a tentativa por excesso de tentativas e não verifica a conta

### Requirement: Reenvio de código com cooldown
O sistema SHALL expor uma operação de reenvio de código para contas ainda não verificadas, que invalida qualquer código anterior daquela conta e gera um novo. Reenvios para a mesma conta SHALL respeitar um intervalo mínimo de 60 segundos entre solicitações.

#### Scenario: Reenvio dentro do intervalo mínimo
- **WHEN** o usuário solicita um novo código antes de 60 segundos desde o envio anterior
- **THEN** o sistema recusa o reenvio informando que é necessário aguardar, sem gerar um novo código

#### Scenario: Reenvio após o intervalo mínimo
- **WHEN** o usuário solicita um novo código depois de 60 segundos da última solicitação
- **THEN** um novo código de 6 dígitos é gerado e enviado, e o código anterior deixa de ser válido

#### Scenario: Reenvio para conta já verificada
- **WHEN** é solicitado reenvio de código para uma conta cujo email já está verificado
- **THEN** o sistema recusa a solicitação informando que a conta já está verificada

### Requirement: Login bloqueado para conta não verificada
O login tradicional (email e senha) SHALL ser recusado para contas com email ainda não verificado, e a recusa SHALL ser distinguível de uma conta bloqueada por um administrador.

#### Scenario: Tentativa de login com conta não verificada
- **WHEN** um usuário com email ainda não verificado tenta logar com email e senha corretos
- **THEN** o acesso é negado
- **AND** a resposta indica que é necessário confirmar o email antes de entrar

### Requirement: Envio de email é desligável em desenvolvimento
O sistema SHALL permitir desativar o envio real de emails por configuração, sem impedir o restante do fluxo de geração e validação de código. Quando o envio estiver desligado, o código gerado SHALL NOT ser exposto na resposta da API em produção.

#### Scenario: Envio de email desligado
- **WHEN** a configuração de envio de email está desligada
- **THEN** o código ainda é gerado e armazenado normalmente, mas nenhum email real é disparado
