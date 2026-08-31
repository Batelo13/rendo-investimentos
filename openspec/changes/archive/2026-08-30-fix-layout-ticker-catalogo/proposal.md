## Why

Usuário reportou (com print) que o campo do ticker no catálogo de ações precisava de correção após a ação ser cadastrada. Investigando `renderAcoes()` em `dashboard.js`: a coluna do ticker usava `<td class="acao-ticker-col">`, mas `.acao-ticker-col` define `display: flex`. Um `<td>` é sempre `display: table-cell` no contexto de uma `<table>` — a regra de flex nunca chegava a ser aplicada, então o logo (`acaoLogoHTML`) e o texto do ticker não ficavam alinhados lado a lado como pretendido pelo spec `logo-catalogo-acoes`.

## What Changes

- **BUGFIX**: o conteúdo da célula do ticker passa a ficar dentro de um `<div class="acao-ticker-col">` interno ao `<td>`, preservando `display: table-cell` na célula e permitindo que o `display: flex` da classe funcione de fato.
- Teste novo (`DashboardAcoesMarkupTest`) trava essa marcação no `dashboard.js` para não regredir.

## Capabilities

### New Capabilities
(nenhuma)

### Modified Capabilities
(nenhuma — restaura o comportamento já descrito em `logo-catalogo-acoes` (logo exibido junto ao ticker); não muda o contrato, só a implementação)

## Impact

- `dashboard.css`: comentário explicando por que o wrapper interno é necessário.
- `dashboard.js`: `renderAcoes()` — `<td class="acao-ticker-col">` vira `<td><div class="acao-ticker-col">`.
- `DashboardAcoesMarkupTest.java` (novo): garante a marcação correta.
