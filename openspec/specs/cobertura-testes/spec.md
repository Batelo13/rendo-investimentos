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

