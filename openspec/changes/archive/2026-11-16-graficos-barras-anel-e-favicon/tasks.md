## 1. Gráficos

- [x] 1.1 Card "Valor por ação" com barras animadas e a maior em destaque. Teste: Playwright com 4 compras mostra as barras em ordem (AAPL > ITUB4 > PETR4 > VALE3).
- [x] 1.2 Anel "Capital investido" conta até o percentual. Teste: Playwright mede 80% (posições ÷ patrimônio) e `stroke-dashoffset` igual a 100 − %.
- [x] 1.3 Sem posições, o card mostra o vazio e o anel fica em 0%.
- [x] 1.4 Tema claro e escuro. Teste: screenshots nos dois temas.

## 2. Favicon

- [x] 2.1 `<link rel="icon">` nas 3 páginas. Teste: Playwright lê o href e recebe 200 sem login.
- [x] 2.2 A suíte `mvnw test` continua verde.
