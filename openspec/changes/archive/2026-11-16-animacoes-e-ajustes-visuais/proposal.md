## Why

Três pontos vistos usando o sistema:

- O logo do AAPL sumiu do catálogo. A URL está salva e responde, mas qualquer falha pontual de carga no navegador (bloqueador, rede) dispara o `onerror="this.remove()"`, e a linha fica sem nada.
- Na tela de login e cadastro (janela de 1890x907), o painel lateral tem 759px de conteúdo num cartão de 718px. Por isso o topo ("Ainda não tem uma conta? / Criar conta") fica cortado. Os blobs do fundo esticam a página, que passa a rolar e mostra uma faixa de fundo cortada. A marca d'água da carteira aparece como uma "bolha" atrás dos widgets.
- O usuário pediu animações no estilo do vídeo de referência: números contando, gráficos se desenhando, cards entrando em sequência e troca de seção com transição.

## What Changes

- O logo ganha um monograma (a inicial do ticker) atrás da imagem. Se a imagem falhar, o monograma continua visível.
- Tela de login e cadastro:
  - o espaçamento do painel lateral foi reduzido para caber no cartão (com `safe center`);
  - os blobs ficam recortados dentro da página (`overflow: clip`);
  - a marca d'água saiu.
- Dashboard:
  - os indicadores contam do valor anterior até o novo;
  - a linha do gráfico de rendimento e as sparklines se desenham;
  - a troca de seção entra com fade e blur em cascata;
  - os cards sobem no hover.
- Tudo respeita `prefers-reduced-motion`, sem animação.

## Impact

- Front-end apenas: `login.css`, `login.html`, `dashboard.js`, `dashboard.css` e `dashboard.html`.
- Sem mudança de API, banco ou dependência.
