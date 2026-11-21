## ADDED Requirements

### Requirement: Simulador demonstrativo
A página inicial SHALL ter uma seção "Experimente" em que o visitante escolhe até 4 ações demonstrativas e distribui R$ 100.000 entre elas. A soma alocada SHALL NOT passar de R$ 100.000, e o restante SHALL aparecer como caixa. Para os cenários Alta, Lateral e Queda, a seção SHALL mostrar o patrimônio após 12 meses, um gráfico e a alocação, sem enviar nada ao servidor.

#### Scenario: Limite de R$ 100.000
- **WHEN** o visitante tenta alocar mais do que o saldo livre numa ação, pelo mouse ou pelo teclado
- **THEN** o slider para no saldo livre e o caixa mostra "R$ 0"

#### Scenario: Ate 4 acoes
- **WHEN** 4 ações estão escolhidas
- **THEN** as demais ficam indisponíveis e aparece "Escolha até 4 ações."; ao desmarcar uma, elas voltam a ficar disponíveis e o valor dela volta ao caixa

#### Scenario: Carteira toda em caixa
- **WHEN** nenhuma ação está escolhida
- **THEN** o patrimônio é R$ 100.000,00 em qualquer cenário

#### Scenario: Cenarios comparaveis
- **WHEN** o visitante alterna entre Alta, Lateral e Queda com a mesma carteira
- **THEN** o patrimônio de Alta é maior que o de Lateral, que é maior que o de Queda, e voltar a um cenário mostra o mesmo valor de antes

### Requirement: Rotulos demonstrativos no simulador
O simulador SHALL exibir o selo "Dados demonstrativos", a legenda "Série ilustrativa: não é histórico real nem previsão." e a cotação "US$ 1 = R$ 5,40 (cotação demonstrativa)", e SHALL descrever o resultado em texto num resumo acessível.

#### Scenario: Leitor de tela
- **WHEN** o visitante troca de cenário
- **THEN** o resumo anuncia o cenário, o patrimônio após 12 meses e o caixa
