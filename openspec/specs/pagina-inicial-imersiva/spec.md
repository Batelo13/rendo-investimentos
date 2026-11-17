# pagina-inicial-imersiva Specification

## Purpose
TBD - created by archiving change pagina-inicial-imersiva. Update Purpose after archive.
## Requirements
### Requirement: Narrativa controlada pelo scroll
A página inicial SHALL ter uma seção imersiva com uma cena de tela cheia fixa (sticky), cujo progresso SHALL acompanhar a posição do scroll nos dois sentidos. Com vídeo disponível, a posição do vídeo SHALL ser `progresso × duração`, aplicada só depois de os metadados serem carregados.

#### Scenario: Usuário rola para baixo e volta
- **WHEN** o usuário rola até metade da seção e depois volta ao topo
- **THEN** a cena avança até o momento "Evolução" e retrocede até o "Começo da jornada", sem tocar sozinha

#### Scenario: Vídeo indisponível
- **WHEN** o arquivo de vídeo não está configurado ou falha ao carregar
- **THEN** a mesma narrativa é exibida na cena ilustrada, sem erro e sem área vazia

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

