## 1. Bugfix: layout do ticker quebrado

- [x] 1.1 Confirmar causa raiz: `.acao-ticker-col { display: flex }` aplicada direto num `<td>` (que é sempre `table-cell`) nunca tem efeito
- [x] 1.2 Mover a classe para um `<div>` interno ao `<td>` em `renderAcoes()` (`dashboard.js`)
- [x] 1.3 Comentar em `dashboard.css` por que o wrapper é necessário
- [x] 1.4 Adicionar `DashboardAcoesMarkupTest` travando a marcação correta

## 2. Verificação

- [x] 2.1 `mvnw -Dtest=DashboardAcoesMarkupTest test` passa
- [x] 2.2 Confirmado que não há outro template duplicando essa marcação (`acao-ticker-col` só aparece em css/js/teste)
