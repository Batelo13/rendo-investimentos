# cobertura-testes Specification

## Purpose
TBD - created by archiving change admin-jwt-rbac-cobertura. Update Purpose after archive.
## Requirements
### Requirement: Portao de cobertura no build
`./mvnw verify` SHALL gerar o relatório JaCoCo (`target/site/jacoco/index.html`) e falhar quando a cobertura de linhas ou de ramos ficar abaixo do portão versionado no `pom.xml`. O portão SHALL NOT ser reduzido, e as exclusões SHALL ser justificadas.

#### Scenario: Cobertura acima do portão
- **WHEN** `./mvnw verify` roda com a suíte atual
- **THEN** o build termina com sucesso e o relatório é gerado

#### Scenario: Portão mais alto que a cobertura
- **WHEN** `./mvnw verify -Djacoco.minimum.branch.coverage=0.95` roda
- **THEN** o build falha na checagem de cobertura, mesmo com todos os testes passando

### Requirement: Testes e cobertura no CI
O repositório SHALL rodar `./mvnw clean verify` no GitHub Actions a cada push na branch de desenvolvimento e a cada pull request para a `main`. A execução SHALL falhar quando um teste falhar ou a cobertura ficar abaixo do portão, e SHALL publicar o relatório JaCoCo como artefato mesmo quando falhar.

#### Scenario: Push com a suíte verde
- **WHEN** um commit é enviado para a `27-develop` e todos os testes passam acima do portão
- **THEN** o job "Testes e cobertura" termina com sucesso e o artefato `relatorio-cobertura` fica disponível

#### Scenario: Regressão de teste ou de cobertura
- **WHEN** um pull request para a `main` traz um teste falhando ou cobertura abaixo do portão
- **THEN** o job falha e o relatório continua publicado para análise

