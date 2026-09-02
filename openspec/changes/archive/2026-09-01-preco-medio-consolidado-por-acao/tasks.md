## Tasks

- [x] Adicionar `consolidarPosicoesPorAcao()` em `dashboard.js`, agrupando `state.posicoes` por `acaoTicker` (soma de quantidade/valorInvestido/valorAtual, lista de corretoras).
- [x] Atualizar `renderVisaoGeral()` para consolidar antes de montar a mini-lista, usando o preço médio ponderado (`valorInvestido / quantidade`) por grupo.
- [x] Exibir contagem de corretoras (`×N` / "N corretoras") quando o grupo combinar mais de uma corretora; manter nome da corretora quando for só uma.
- [x] Remover o menu kebab/"Vender" da mini-lista (ação ambígua numa linha consolidada).
- [x] Ajustar `grid-template-columns` de `.mini-row` em `dashboard.css` (base + 2 breakpoints) para remover a coluna do botão removido.
- [x] Verificar em navegador real: cadastrar 2 corretoras, comprar a mesma ação (PETR4) em ambas com preços diferentes (100@R$40 + 50@R$50), confirmar que a Visão Geral mostra 1 linha consolidada (150 un., preço médio R$43,33, selo "×2") e que "Minhas posições" continua com 2 linhas separadas.
- [x] Rodar suite de testes completa (`mvnw test`) para confirmar que nenhum teste backend foi afetado (mudança é 100% frontend).
