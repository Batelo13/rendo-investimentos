## Por quê

A revisão da landing deixou cinco pendências visuais: o botão magnético volta num salto e reage ao toque em notebooks com tela sensível; o tour pode marcar o passo errado ao rolar para cima; o satélite da abertura fica cortado entre 1100 e 1129 px; o cartão da abertura aparece vazio sem JavaScript; e as cores do anel do simulador trocam ao desmarcar uma ação.

## O que muda

- **Botão magnético:** a volta ao lugar fica animada (a `transition` do `.lp-botao` passa a incluir `translate`). Os efeitos de ponteiro (ímã, luz, inclinação) só reagem ao mouse (`pointerType === "mouse"`), nunca a toque ou caneta.
- **Tour:** o passo ativo é o último cujo topo já passou do meio da tela, recalculado a cada quadro de rolagem, em vez de depender de qual passo entrou numa faixa estreita.
- **Satélites:** só aparecem a partir de 1130 px de largura, no CSS e no `landing.js`.
- **Cartão sem JS:** o HTML traz o estado final (linha do gráfico, fatias do anel, chips com cor). Com JS, `<html class="js">` (marcada pelo `tema-inicial.js`) esconde esse estado até a animação.
- **Simulador:** cada ação guarda a sua cor enquanto estiver escolhida, e uma ação nova recebe a primeira cor livre.

## Impacto

Só front-end da landing: `index.html`, `landing.css`, `landing.js`, `landing-tour.js`, `landing-simulador.js`, `motion.js`, `tema-inicial.js` e `landing_regression.py`. Nenhum endpoint e nenhuma mudança no banco.
