## Purpose

Exibe o logo da empresa junto de cada ação cadastrada, facilitando o reconhecimento visual no catálogo e na tela de detalhe.

## Requirements

### Requirement: Logo da ação no catálogo e no detalhe
O sistema SHALL exibir o logo da empresa, quando disponível, junto ao ticker no catálogo de ações e na tela de detalhe.

#### Scenario: Ação do mercado Brasil com logo disponível
- **WHEN** uma ação do mercado Brasil é cadastrada ou tem a cotação atualizada
- **THEN** o sistema captura o logo retornado pela fonte de cotação e o exibe junto ao ticker

#### Scenario: Ação do mercado EUA com logo disponível
- **WHEN** uma ação do mercado EUA é cadastrada ou tem a cotação atualizada
- **THEN** o sistema associa um logo baseado no ticker e o exibe junto ao ticker

#### Scenario: Logo indisponível não quebra a exibição
- **WHEN** o logo de uma ação não pode ser carregado (indisponível ou ticker sem logo correspondente)
- **THEN** o catálogo e o detalhe continuam exibindo normalmente o ticker e os demais dados, sem espaço em branco quebrado ou ícone de imagem ausente
