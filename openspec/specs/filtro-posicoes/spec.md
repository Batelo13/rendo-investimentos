## Purpose

Permite ao usuário restringir a lista de "Minhas posições" no dashboard a um subconjunto relevante, combinando filtros por mercado, corretora e resultado (lucro/prejuízo) com a busca textual já existente.

## Requirements

### Requirement: Filtro de posições por mercado
O sistema SHALL permitir filtrar a lista de posições exibida por mercado da ação (BRASIL ou EUA).

#### Scenario: Selecionar um mercado específico
- **WHEN** o usuário seleciona "EUA" no filtro de mercado na tela "Minhas posições"
- **THEN** a tabela exibe apenas as posições cujo ticker corresponde a uma ação cadastrada com mercado EUA

#### Scenario: Opção "Todos" no filtro de mercado
- **WHEN** o usuário seleciona a opção padrão ("Todos") no filtro de mercado
- **THEN** nenhuma posição é excluída por esse critério

### Requirement: Filtro de posições por corretora
O sistema SHALL permitir filtrar a lista de posições exibida por corretora, com as opções geradas dinamicamente a partir das corretoras presentes nas posições do usuário.

#### Scenario: Selecionar uma corretora específica
- **WHEN** o usuário seleciona uma corretora no filtro de corretora
- **THEN** a tabela exibe apenas as posições cujo `corretoraNome` corresponde exatamente à corretora selecionada

#### Scenario: Lista de corretoras reflete as posições atuais
- **WHEN** a tela "Minhas posições" é carregada ou recarregada
- **THEN** as opções do filtro de corretora correspondem ao conjunto de corretoras distintas presentes em `state.posicoes`, sem incluir corretoras sem nenhuma posição do usuário

### Requirement: Filtro de posições por resultado
O sistema SHALL permitir filtrar a lista de posições exibida por resultado (lucro, prejuízo ou neutro), calculado como `valorAtual - valorInvestido` de cada posição.

#### Scenario: Filtrar apenas posições com lucro
- **WHEN** o usuário seleciona "Lucro" no filtro de resultado
- **THEN** a tabela exibe apenas posições cujo `valorAtual - valorInvestido` é maior que zero

#### Scenario: Filtrar apenas posições com prejuízo
- **WHEN** o usuário seleciona "Prejuízo" no filtro de resultado
- **THEN** a tabela exibe apenas posições cujo `valorAtual - valorInvestido` é menor que zero

#### Scenario: Filtrar posições neutras
- **WHEN** o usuário seleciona "Neutro" no filtro de resultado
- **THEN** a tabela exibe apenas posições cujo `valorAtual - valorInvestido` é exatamente igual a zero

### Requirement: Combinação de filtros
O sistema SHALL aplicar todos os filtros ativos (mercado, corretora, resultado e busca textual) em conjunto, exibindo apenas as posições que satisfazem simultaneamente todos os critérios selecionados.

#### Scenario: Múltiplos filtros ativos ao mesmo tempo
- **WHEN** o usuário seleciona "EUA" no filtro de mercado, uma corretora específica no filtro de corretora, e digita um termo na busca textual
- **THEN** a tabela exibe apenas as posições que atendem aos três critérios simultaneamente

#### Scenario: Nenhuma posição satisfaz os filtros combinados
- **WHEN** a combinação de filtros ativos não corresponde a nenhuma posição do usuário
- **THEN** o sistema exibe o estado vazio já usado quando a busca textual não encontra resultados, sem tratar isso como erro

### Requirement: Contador de posições reflete os filtros aplicados
O sistema SHALL atualizar o contador de posições exibido no painel para refletir a quantidade de posições visíveis após a aplicação dos filtros, mantendo separadamente a contagem total de posições do usuário quando exibida.

#### Scenario: Contador acompanha o resultado filtrado
- **WHEN** os filtros ativos reduzem a lista visível para um subconjunto das posições do usuário
- **THEN** o contador de itens exibidos na tabela reflete o número de posições visíveis após o filtro, não o total de posições do usuário
