## 1. Design tokens

- [x] 1.1 Reescrever `tokens.css` com a paleta pedida, escalas de radius/spacing/transition
- [x] 1.2 Migrar `dashboard.css`, `login.css`, `loading.css` e os `var(--rendo-color-*)` inline em `dashboard.js` pros novos nomes

## 2. Sidebar e topbar

- [x] 2.1 Ícone SVG inline por item de navegação + estado ativo por barra lateral (não mais "botão bolha")
- [x] 2.2 Sidebar mais densa (padding/gap reduzidos), rodapé (Tema/Sair) com ícone + hover consistente
- [x] 2.3 Topbar: "Atualizar" vira ghost com ícone, copy do subtítulo atualizada

## 3. Indicadores + Resultado da carteira

- [x] 3.1 Fundir os 4 stat cards + o painel "Resultado da carteira" numa única grade de 6 tiles
- [x] 3.2 Remover glow (`text-shadow`) dos valores financeiros em todo o dashboard (não só Visão Geral)

## 4. Gráfico de evolução patrimonial

- [x] 4.1 Título "Evolução patrimonial", altura maior, grid horizontal discreto, área sob a linha
- [x] 4.2 Pills de período (1D/1M/3M/6M/1A/Tudo) com filtro client-side por data
- [x] 4.3 Tooltip on-hover (data + valor do ponto mais próximo)
- [x] 4.4 Skeleton de carregamento ao atualizar os dados

## 5. Minhas posições (mini-lista da Visão Geral)

- [x] 5.1 Mini-tabela em grid: logo + ticker + empresa, corretora, quantidade, preço médio, resultado, valor
- [x] 5.2 Colunas secundárias somem em telas estreitas (`@media`)

## 6. Ícones e botões

- [x] 6.1 Substituir todo emoji funcional por ícone SVG (helper `icone()` no JS + inline no HTML)
- [x] 6.2 Flags de país viram badge de texto ("BR"/"US")
- [x] 6.3 Classes `.btn-ghost`/`.btn-danger` novas; classes já usadas pelo JS preservadas

## 7. Verificação

- [x] 7.1 `mvnw compile`/`mvnw test` passam (nenhum `.java` alterado)
- [x] 7.2 Verificação manual no navegador: cadastro de corretora/ação, compra com confirmação SweetAlert2, saldo/posições/resultado atualizando corretamente, tooltip e pills do gráfico funcionando, tema claro/escuro, todas as abas herdando os tokens novos
- [x] 7.3 Console do navegador sem erros novos
- [ ] 7.4 Responsividade em 1920/1440/1366/1024/tablet/mobile — verificada por inspeção do CSS (mesmo padrão de breakpoint já comprovado no arquivo); redimensionamento visual da janela não funcionou na ferramenta de automação desta sessão, recomendado teste manual rápido do usuário
