## REMOVED Requirements

### Requirement: Narrativa controlada pelo scroll
**Reason**: a cena não usa mais vídeo (decisão do usuário); o requisito citava a sincronização do vídeo e o fallback "Vídeo indisponível".
**Migration**: substituído por "Narrativa ilustrada controlada pelo scroll".

## ADDED Requirements

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
