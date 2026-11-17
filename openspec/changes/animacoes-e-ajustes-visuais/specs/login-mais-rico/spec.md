## ADDED Requirements

### Requirement: Painel lateral cabe no cartão sem rolagem
A tela de login e cadastro SHALL exibir o conteúdo completo do painel lateral dentro do cartão, incluindo o botão de troca no topo, e a página SHALL NOT rolar por causa dos elementos decorativos de fundo.

#### Scenario: Janela de desktop comum
- **WHEN** a tela de login é aberta numa janela de 1890x907
- **THEN** o botão "Criar conta" do topo do painel fica visível e a página não tem rolagem vertical
