## ADDED Requirements

### Requirement: Menu de ações por posição na Visão Geral
Cada linha da lista de posições na Visão Geral SHALL ter um menu (⋮) com a ação "Vender", reaproveitando exatamente o mesmo fluxo de venda já existente na página completa de posições.

#### Scenario: Vender a partir da Visão Geral
- **WHEN** o usuário abre o menu de uma posição na Visão Geral e clica em "Vender"
- **THEN** o mesmo modal de venda usado na página "Minhas posições" abre, pré-carregado com os dados corretos daquela posição

### Requirement: Sparkline no card de resultado não realizado
O card "Lucro/Prejuízo não realizado" SHALL exibir uma sparkline construída a partir da mesma série de rendimento já carregada para o gráfico principal, sem nenhuma chamada adicional de API.

#### Scenario: Série de rendimento tem 2+ pontos
- **WHEN** a carteira tem 2 ou mais pontos na série de rendimento
- **THEN** o card de resultado não realizado mostra uma sparkline refletindo a tendência recente dessa série

### Requirement: Widget de índice IBOV na sidebar
A sidebar do dashboard SHALL exibir um widget com a cotação real do índice IBOVESPA (pontos e variação percentual do dia), obtida de um endpoint próprio do backend. Se a consulta externa falhar, o widget SHALL ficar oculto em vez de quebrar a página ou mostrar dado inventado.

#### Scenario: Consulta ao índice funciona
- **WHEN** o endpoint de índice responde com sucesso
- **THEN** a sidebar mostra os pontos e a variação percentual reais do IBOVESPA

#### Scenario: Consulta ao índice falha
- **WHEN** o endpoint de índice falha ou está indisponível
- **THEN** o widget do IBOV não é exibido, e o restante do dashboard continua funcionando normalmente

### Requirement: Aviso de atraso nos dados de posições
A lista de posições (Visão Geral e página completa) SHALL exibir um aviso indicando que os dados podem ter atraso.

#### Scenario: Visualizando a lista de posições
- **WHEN** o usuário visualiza a lista de posições
- **THEN** um aviso "Os dados podem ter até 15 minutos de atraso." é exibido próximo à lista

## MODIFIED Requirements

### Requirement: Preservação de comportamento e dados existentes
O sistema SHALL manter inalterados todos os endpoints, regras de negócio, cálculos e dados exibidos no dashboard — a mudança é exclusivamente de apresentação visual. Nenhum widget ou indicador SHALL exibir dados de mercado inventados ou fixos; dados de mercado externos (ex.: índices como IBOV) só SHALL aparecer quando vierem de um endpoint próprio do backend que consulte a fonte real, e SHALL ficar ocultos se essa consulta falhar.

#### Scenario: Fluxos existentes continuam funcionando
- **WHEN** o usuário cadastra corretora/ação, compra/vende uma ação, navega entre as abas ou alterna o tema claro/escuro
- **THEN** o comportamento e os valores calculados são idênticos aos anteriores ao redesign, apenas com nova apresentação visual

#### Scenario: Dado de mercado externo com endpoint real no backend
- **WHEN** um elemento visual do dashboard exibe um dado de mercado externo (ex.: índice IBOV) e o backend tem um endpoint que consulta esse dado de verdade
- **THEN** o elemento mostra o valor real retornado por esse endpoint, nunca um valor fixo digitado no frontend

#### Scenario: Dado de mercado externo não disponível no backend
- **WHEN** um elemento visual do dashboard exigiria um dado de mercado externo e o backend não tem (ou a consulta ao endpoint que tem falha)
- **THEN** esse elemento não é exibido, em vez de mostrar um valor inventado, fixo ou desatualizado
