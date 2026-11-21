## ADDED Requirements

### Requirement: Titulos de painel com tamanho uniforme
Todos os títulos de painel do dashboard, inclusive o do gráfico de evolução patrimonial, SHALL ter o mesmo tamanho de fonte (16px).

#### Scenario: Usuario abre a visao geral
- **WHEN** o usuário autenticado abre o dashboard
- **THEN** "Valor por ação", "Capital investido", "Evolução patrimonial" e "Minhas posições" aparecem com 16px
