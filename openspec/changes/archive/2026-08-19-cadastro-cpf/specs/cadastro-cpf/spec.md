## Purpose

Garante que todo `Usuario` cadastrado tem um CPF sintaticamente válido (formato + dígito verificador) e único no sistema, sem depender de nenhuma consulta externa — CPF é dado protegido e não existe API pública equivalente à consulta de CNPJ já usada para `Corretora`.

## ADDED Requirements

### Requirement: CPF é obrigatório e validado no cadastro
O sistema SHALL exigir um CPF no cadastro de `Usuario`, rejeitando valores que não tenham exatamente 11 dígitos numéricos ou cujo dígito verificador (algoritmo padrão mod-11) não confira, incluindo o caso degenerado de todos os dígitos iguais.

#### Scenario: CPF ausente
- **WHEN** um cadastro é enviado sem CPF
- **THEN** o sistema rejeita com 400 e uma mensagem indicando que o CPF é obrigatório

#### Scenario: CPF com formato errado
- **WHEN** um cadastro é enviado com um CPF que não tem 11 dígitos numéricos (ex.: contém pontuação ou tem menos dígitos)
- **THEN** o sistema rejeita com 400

#### Scenario: CPF com dígito verificador inválido
- **WHEN** um cadastro é enviado com um CPF de 11 dígitos cujo dígito verificador não confere com o algoritmo padrão
- **THEN** o sistema rejeita com 400

#### Scenario: CPF com todos os dígitos iguais
- **WHEN** um cadastro é enviado com um CPF como `11111111111` (todos os dígitos iguais, caso que passaria no cálculo ingênuo do dígito verificador)
- **THEN** o sistema rejeita com 400

#### Scenario: CPF válido
- **WHEN** um cadastro é enviado com um CPF de 11 dígitos e dígito verificador correto
- **THEN** o cadastro prossegue normalmente

### Requirement: CPF é único no sistema
O sistema SHALL impedir que dois usuários sejam cadastrados com o mesmo CPF.

#### Scenario: CPF duplicado
- **WHEN** um cadastro é enviado com um CPF que já pertence a outro usuário
- **THEN** o sistema rejeita com 409 e uma mensagem indicando que já existe um usuário cadastrado com aquele CPF

### Requirement: CPF é exposto na resposta do cadastro
O sistema SHALL incluir o CPF cadastrado na resposta da API de cadastro/consulta de usuário, no mesmo nível de exposição que o email já recebe.

#### Scenario: Consulta após cadastro
- **WHEN** um usuário é cadastrado com sucesso
- **THEN** a resposta da API inclui o CPF exatamente como foi enviado (11 dígitos, sem pontuação)
