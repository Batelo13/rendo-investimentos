## Por quê

A aula 15 sugere rodar `./mvnw clean verify` (testes + portão de cobertura) a cada push e pull request. Hoje o CI só tem o gate de segurança semanal, e uma regressão de teste ou de cobertura só aparece quando alguém roda a suíte na própria máquina.

## O que muda

- Workflow novo, `.github/workflows/testes.yml`, que roda em push na `27-develop`, em pull request para a `main` e manualmente.
- Executa `./mvnw -B clean verify` com Java 17 e cache do Maven. Falha se um teste falhar ou se a cobertura ficar abaixo do portão do `pom.xml`.
- Publica o relatório JaCoCo (`target/site/jacoco/`) e os relatórios do Surefire como artefatos, mesmo quando falha.

## Impacto

- Nenhum endpoint, nenhuma mudança no banco e nenhuma dependência Maven nova.
- As actions são as mesmas do `seguranca.yml`, fixadas por SHA.
