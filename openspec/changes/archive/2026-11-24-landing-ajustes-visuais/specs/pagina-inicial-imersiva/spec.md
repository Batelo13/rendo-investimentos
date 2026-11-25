## ADDED Requirements

### Requirement: Efeitos de ponteiro so com mouse e volta animada
O botão magnético SHALL voltar ao lugar com transição animada quando o mouse sai. Os efeitos de ponteiro (ímã, luz e inclinação) SHALL reagir somente a ponteiro do tipo mouse, mesmo em telas que também aceitam toque.

#### Scenario: Volta animada
- **WHEN** o mouse sai do botão magnético
- **THEN** a transição do botão inclui a propriedade `translate`

#### Scenario: Toque em tela híbrida
- **WHEN** um toque gera movimento de ponteiro sobre o botão num dispositivo com mouse
- **THEN** o botão não se desloca

### Requirement: Tour marca o passo correto em qualquer direcao
O tour SHALL marcar como ativo o último passo cujo topo já passou do meio da tela, rolando para baixo, para cima ou em saltos.

#### Scenario: Salto para o começo de um passo anterior
- **WHEN** a página salta do passo 3 direto para o começo do passo 1
- **THEN** só o passo 1 fica ativo

### Requirement: Abertura sem corte e sem JavaScript
Os satélites da abertura SHALL aparecer só a partir de 1130 px de largura, sem nenhum corte. Sem JavaScript, o cartão da abertura SHALL mostrar a linha do gráfico, o anel de alocação e os chips com as suas cores.

#### Scenario: Largura de 1110 px
- **WHEN** a página é aberta com 1110 px de largura
- **THEN** os satélites não são exibidos

#### Scenario: Sem JavaScript
- **WHEN** a página é aberta com JavaScript desligado
- **THEN** a linha do gráfico tem pontos e traço visível, os chips da alocação estão visíveis, nada cobre o cartão e o título da jornada aparece

### Requirement: Cores estaveis no anel do simulador
Cada ação escolhida no simulador SHALL manter a mesma cor no anel e no chip enquanto continuar escolhida.

#### Scenario: Desmarcar a primeira ação
- **WHEN** a primeira ação escolhida é desmarcada
- **THEN** as outras ações mantêm as cores que tinham
