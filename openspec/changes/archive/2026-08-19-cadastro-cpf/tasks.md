## 1. Validação de CPF

- [x] 1.1 Criar anotação `@CPF` (Bean Validation) em `validation/CPF.java`
- [x] 1.2 Implementar `CpfValidator` (formato + dígito verificador mod-11 + rejeição de dígitos repetidos)
- [x] 1.3 Testes unitários de `CpfValidator` (`CpfValidatorTest`)

## 2. Modelo e persistência

- [x] 2.1 Adicionar campo `cpf` (único, obrigatório) em `Usuario`
- [x] 2.2 Adicionar `UsuarioRepository.findByCpf`

## 3. DTOs e Service

- [x] 3.1 Adicionar `cpf` com `@NotBlank`/`@Pattern`/`@CPF` em `UsuarioRequestDTO`
- [x] 3.2 Adicionar `cpf` em `UsuarioResponseDTO`
- [x] 3.3 Checagem de duplicidade de CPF em `UsuarioService.cadastrar()`

## 4. Testes de integração

- [x] 4.1 Fixture `CpfTestFixtures` (gerador de CPF sintético válido único por chamada)
- [x] 4.2 Cobrir CPF inválido (400) e CPF duplicado (409) em `UsuarioAuthIntegrationTest`
- [x] 4.3 Ajustar fixture de `OperacaoIntegrationTest` para incluir CPF válido

## 5. Frontend

- [x] 5.1 Campo CPF no formulário de cadastro (`login.html`)
- [x] 5.2 Enviar CPF em dígitos puros e validar comprimento ao vivo (`login.js`)

## 6. Verificação

- [x] 6.1 Suíte completa (`./mvnw.cmd test`) — 48 testes, 0 falhas
- [x] 6.2 Verificação manual no navegador (campo renderiza, validação ao vivo, CPF inválido/duplicado rejeitados, CPF válido aceito, payload em dígitos puros confirmado no network tab)
