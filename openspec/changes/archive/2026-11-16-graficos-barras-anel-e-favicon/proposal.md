## Why

O usuário pediu duas coisas:

- O gráfico de barras e o anel de progresso do segundo vídeo de referência, com o mesmo efeito de entrada e nas cores do Rendo.
- O logo do Rendo na aba do navegador: hoje a aba mostra o ícone genérico.

## What Changes

A visão geral ganha um card **"Valor por ação"**:
- uma barra por ação, com o valor atual em R$ (USD convertido pela taxa da própria ação), da maior para a menor;
- a maior barra fica destacada;
- as barras crescem em cascata.

Ao lado entra o anel **"Capital investido"**: a porcentagem do patrimônio aplicada em ações, que conta até o valor enquanto o anel se preenche.

As cores vêm só de tokens, então os dois cards funcionam no tema claro e no escuro.

O favicon passa a ser o SVG da carteira do Rendo (`/images/favicon.svg`, rota que já era pública) nas páginas inicial, login e dashboard.

## Impact

Front-end apenas. Os dados vêm do que o dashboard já carrega (posições e saldo), sem endpoint novo. A `SecurityConfig` não muda, porque `/images/**` já estava liberado.
