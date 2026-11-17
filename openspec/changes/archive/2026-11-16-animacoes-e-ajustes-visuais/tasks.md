## 1. Login e cadastro

- [x] 1.1 Fazer o painel lateral caber no cartão a 1890x907. Teste: Playwright mede `scrollHeight <= clientHeight` nos dois painéis.
- [x] 1.2 A página não rola por causa dos blobs. Teste: Playwright mede a altura do documento igual à da janela.
- [x] 1.3 Remover a marca d'água por trás dos widgets.

## 2. Logo

- [x] 2.1 Monograma atrás do logo, que fica visível quando a imagem falha. Teste: Playwright com a URL do logo bloqueada mostra a inicial.

## 3. Animações do dashboard

- [x] 3.1 Indicadores contam até o valor e o texto final é igual à formatação. Teste: Playwright lê o valor final.
- [x] 3.2 A linha do gráfico e as sparklines se desenham.
- [x] 3.3 A troca de seção usa fade e blur em cascata, e os cards sobem no hover.
- [x] 3.4 Com `prefers-reduced-motion`, o valor final aparece direto. Teste: Playwright com `reduced_motion="reduce"`.
- [x] 3.5 A suíte `mvnw test` continua verde.
