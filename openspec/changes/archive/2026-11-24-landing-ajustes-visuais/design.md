## Decisões

1. **Filtro de mouse no `porQuadro`.** Os três efeitos de ponteiro passam por essa mesma função, então uma guarda ali cobre todos. A media query `(hover: hover) and (pointer: fine)` continua decidindo se os efeitos são ligados. O `pointerType` resolve notebooks híbridos, onde a query é verdadeira e o toque também gera `pointermove`.
2. **Tour por posição, não por interseção.** O `IntersectionObserver` com faixa de 5% entrega mais de um passo "visível" no mesmo lote quando a faixa cai na divisa entre dois passos, e o último processado vence (reproduzido: salto para o começo do passo 1 deixava o passo 2 marcado). A regra "último passo cujo topo ≤ meio da tela" é determinística e funciona nos dois sentidos. O custo é ler 4 retângulos por quadro de rolagem, com listener passivo e um `requestAnimationFrame` por quadro.
3. **Satélites a partir de 1130 px.** A posição é `50% + 215px` mais 210px de largura mais até 12px de parallax, o que passa da borda do palco até cerca de 1125 px. Subir o corte é mais simples e seguro do que reposicionar e arriscar sobrepor o cartão.
4. **Sem JS = estado final.** O estado final fica no próprio HTML, com valores calculados pela mesma fórmula do `landing.js`. A classe `js` no `<html>` sai do `tema-inicial.js`, que já roda de forma síncrona no `<head>` antes do primeiro desenho; a CSP proíbe script inline.
5. **Cor estável no simulador:** um `Map` ticker → cor. A cor é liberada quando a ação sai e entregue à primeira ação nova sem cor.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Não se aplica. |
| Tampering | Nada é enviado ao servidor. |
| Repudiation | Não se aplica. |
| Information disclosure | Só dados demonstrativos, já visíveis. |
| Denial of service | O listener de rolagem é passivo e limitado a um cálculo por quadro. |
| Elevation of privilege | A CSP continua restrita: nenhum script inline (a classe `js` vem de arquivo externo). |
