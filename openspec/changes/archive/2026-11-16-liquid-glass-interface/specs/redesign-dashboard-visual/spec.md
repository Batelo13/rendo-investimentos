## ADDED Requirements

### Requirement: Dashboard organizado em planos de profundidade
O dashboard SHALL organizar fundo, navegação, resumos, gráficos, tabelas e
modais em planos visuais distintos de liquid glass, mantendo a ordem funcional,
os dados e as ações já existentes.

#### Scenario: Visualizar a Visão Geral
- **WHEN** o usuário abre a Visão Geral
- **THEN** sidebar, resumo financeiro, gráfico e posições apresentam profundidade e agrupamento claros sem remover indicadores ou alterar valores

#### Scenario: Abrir um modal do dashboard
- **WHEN** o usuário abre um modal de cadastro, compra, venda, transferência ou detalhe
- **THEN** o modal aparece sobre um plano inferior suavemente desfocado e conserva os mesmos campos, validações e ações anteriores

### Requirement: Microinterações preservam leitura de dados
O dashboard SHALL usar elevação, reflexo e transições apenas em elementos
interativos, sem deslocar linhas de tabela ou alterar a leitura de dados
financeiros.

#### Scenario: Passar o cursor sobre um card interativo
- **WHEN** o usuário passa o cursor sobre um card que possui uma ação
- **THEN** o card recebe elevação e destaque discretos sem mover elementos vizinhos

#### Scenario: Passar o cursor sobre uma linha de tabela
- **WHEN** o usuário passa o cursor sobre uma linha de dados
- **THEN** a linha recebe somente realce de superfície ou borda, permanecendo alinhada com as demais colunas

