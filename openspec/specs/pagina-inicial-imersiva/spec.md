# pagina-inicial-imersiva Specification

## Purpose
TBD - created by archiving change pagina-inicial-imersiva. Update Purpose after archive.
## Requirements
### Requirement: Momentos de texto com transição
A seção imersiva SHALL exibir quatro momentos de texto, um dominante por vez, que entram e saem com opacidade, deslocamento e desfoque conforme o progresso. Os botões SHALL apontar para rotas existentes (`/login`, `/login?criarConta=1` ou âncoras da própria página).

#### Scenario: Último momento
- **WHEN** o progresso passa de 75%
- **THEN** aparece "Decisões de hoje constroem o futuro de amanhã." com o botão "Construir meu futuro", que leva a `/login?criarConta=1`

### Requirement: Conteúdo fiel ao produto
As seções depois da narrativa SHALL descrever apenas funcionalidades existentes da Rendo, e SHALL deixar claro que é uma simulação com saldo fictício, sem promessa de rentabilidade.

#### Scenario: Visitante lê os recursos
- **WHEN** o visitante chega às seções de recursos e segurança
- **THEN** só vê funcionalidades e proteções implementadas no sistema

### Requirement: Movimento reduzido e acessibilidade
Com `prefers-reduced-motion`, a página SHALL NOT sincronizar mídia com o scroll e SHALL exibir uma composição estática com todo o conteúdo dos momentos. A mídia decorativa SHALL ficar oculta para leitores de tela, e todos os links SHALL ser alcançáveis por teclado com foco visível.

#### Scenario: Movimento reduzido ativo
- **WHEN** o sistema operacional pede movimento reduzido
- **THEN** os quatro momentos aparecem como texto em sequência, e a cena fica parada

### Requirement: Narrativa ilustrada controlada pelo scroll
A página inicial SHALL ter uma seção imersiva com uma cena de tela cheia fixa (sticky), cujo progresso SHALL acompanhar a posição do scroll nos dois sentidos. A cena SHALL ser ilustrada, sem vídeo.

#### Scenario: Usuário rola para baixo e volta
- **WHEN** o usuário rola até metade da seção e depois volta ao topo
- **THEN** a cena avança até o momento "Evolução" e retrocede até o "Começo da jornada", sem tocar sozinha

#### Scenario: Pedido de video
- **WHEN** um visitante sem sessão pede qualquer caminho em `/videos/`
- **THEN** o sistema responde 401, porque a página não serve mais vídeo

### Requirement: Cena da carteira demonstrativa
A abertura SHALL mostrar um cartão de carteira com o rótulo visível "Dados demonstrativos", cujo conteúdo SHALL acompanhar o progresso: saldo de R$ 100.000,00 no começo, alocação em 4 ações no planejamento, linha de evolução ilustrativa na evolução e cartões de posições e operações no futuro.

#### Scenario: Começo e fim da jornada
- **WHEN** o progresso está no começo e depois no fim da seção
- **THEN** o cartão mostra "R$ 100.000,00" no começo e "R$ 112.400,00" no fim, com os cartões de posições e operações visíveis

#### Scenario: Scroll brusco
- **WHEN** o visitante salta do topo para o fim da seção e volta
- **THEN** a cena termina no estado correspondente a cada posição, sem elementos pela metade

### Requirement: Profundidade com o ponteiro
Com um ponteiro fino (mouse) e sem movimento reduzido, o cartão SHALL inclinar até 6° na direção do ponteiro e exibir um reflexo de luz que o acompanha. Com toque ou movimento reduzido, o cartão SHALL NOT inclinar.

#### Scenario: Toque no celular
- **WHEN** o visitante toca na cena num celular
- **THEN** o cartão não inclina

### Requirement: Contadores animados
Números marcados como contadores SHALL animar de zero até o valor final ao entrar na tela. O valor final SHALL estar no HTML, para que apareça sem JavaScript e com movimento reduzido.

#### Scenario: Sem JavaScript
- **WHEN** a página é aberta com JavaScript desligado
- **THEN** os contadores mostram o valor final, como "R$ 100.000"

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

