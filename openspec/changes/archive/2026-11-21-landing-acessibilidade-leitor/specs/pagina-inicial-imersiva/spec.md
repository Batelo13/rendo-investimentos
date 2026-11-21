## ADDED Requirements

### Requirement: Leitor de tela e contraste nas demonstracoes
As demonstrações da landing SHALL manter contraste AA e anunciar ao leitor de tela só o estado final de cada interação.

#### Scenario: Passo inativo do tour legível
- **WHEN** um passo do tour está inativo
- **THEN** seu parágrafo tem contraste de pelo menos 4,5:1 com o fundo

#### Scenario: Antes e depois distinguidos
- **WHEN** o leitor de tela percorre um card do problema
- **THEN** ouve "Sem a Rendo:" antes do problema e "Com a Rendo:" antes da solução, rótulos que não aparecem na tela

#### Scenario: Aviso do limite de ações anunciado
- **WHEN** o visitante escolhe a 4ª ação no simulador
- **THEN** a região `role="status"` passa a dizer "Escolha até 4 ações."

#### Scenario: Câmbio anuncia o valor convertido
- **WHEN** o visitante alterna a moeda
- **THEN** uma região `role="status"` anuncia "10 ações da AAPL:" seguido do valor final, e o botão não usa `aria-pressed`
