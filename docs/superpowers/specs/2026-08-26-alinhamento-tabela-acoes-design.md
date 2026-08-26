# Alinhamento da tabela de ações

## Contexto

Na tabela **Catálogo de ações**, a classe `.acao-ticker-col` é aplicada diretamente ao elemento `<td>` e define `display: flex`. Isso retira a célula do comportamento nativo de `table-cell`, afetando o alinhamento da linha e da coluna de ações.

## Objetivo

Restaurar o alinhamento horizontal e vertical da linha do catálogo sem alterar dados, eventos, botões ou regras de negócio.

## Solução aprovada

- Manter o `<td>` do ticker como uma célula de tabela normal.
- Adicionar dentro dele um contêiner com a classe `.acao-ticker-col`.
- Aplicar o layout flexível somente nesse contêiner interno para alinhar logotipo e ticker.
- Preservar a marcação e o comportamento da célula `.acoes-col`.

## Arquivos afetados

- `src/main/resources/static/js/dashboard.js`: ajustar o HTML produzido por `renderAcoes()`.
- `src/main/resources/static/css/dashboard.css`: manter o flex limitado ao novo contêiner interno.
- Teste estrutural correspondente: garantir que a classe flexível não volte a ser aplicada diretamente ao `<td>`.

## Fora do escopo

- Alterações no backend, banco de dados ou APIs.
- Redesenho da tabela ou dos botões.
- Mudanças nas demais tabelas do painel.

## Verificação

- Confirmar por teste que o `<td>` contém o contêiner `.acao-ticker-col`.
- Executar a suíte de testes do projeto.
- Reconstruir a imagem da aplicação e verificar visualmente o catálogo em resolução desktop.
