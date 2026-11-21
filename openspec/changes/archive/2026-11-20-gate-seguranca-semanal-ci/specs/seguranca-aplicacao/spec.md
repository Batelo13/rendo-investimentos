## ADDED Requirements

### Requirement: Portao de seguranca recorrente
O repositório SHALL executar semanalmente, e sob demanda, o portão de segurança e o DAST com os mesmos scripts usados localmente, e SHALL marcar a execução como falha quando qualquer um deles reprovar.

#### Scenario: Execucao semanal
- **WHEN** chega a segunda-feira às 06:00 (Brasília)
- **THEN** o gate e o DAST rodam e publicam os relatórios como artefato da execução

#### Scenario: Camada reprovada
- **WHEN** qualquer camada do gate ou o DAST reprova
- **THEN** o job termina com falha e o relatório fica disponível para triagem
