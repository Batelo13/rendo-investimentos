## Decisões

1. **Contraste por opacidade:** mantém o efeito de foco do tour; 0,75 é o menor valor que passa AA com `--lp-texto-2`.
2. **Rótulos ocultos** em vez de `aria-hidden` no "antes": o problema também é conteúdo, então só falta dizer o que é cada parte.
3. **Status sempre no DOM:** alternar `hidden` numa região viva não é anunciado de forma confiável. Fica vazio, com `:empty { display: none }` na tela.
4. **Anúncio do valor final** numa região separada, para a contagem animada não inundar o leitor.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Não se aplica. |
| Tampering | Nada é enviado ao servidor. |
| Repudiation | Não se aplica. |
| Information disclosure | Só textos demonstrativos. |
| Denial of service | Uma troca de `textContent` por interação. |
| Elevation of privilege | CSP mantida: nenhum script inline. |
