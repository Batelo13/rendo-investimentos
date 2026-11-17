## Decisões

- **Sem biblioteca de animação.** A Web Animations API, `requestAnimationFrame` e CSS (`stroke-dashoffset` com `pathLength="1"`) cobrem o vídeo de referência sem nova dependência.
- **Contagem de números.** `contar(el, valor, formatar)` interpola do último valor mostrado (guardado em `el.dataset.valor`) até o novo, com easeOutCubic em cerca de 900ms. A formatação é a mesma que já existia (`fmtMoeda` e `fmtResultado`), então o texto final é idêntico ao de antes.
- **Monograma do logo.** É um `<span>` com a inicial e a `<img>` por cima. O `onerror` só remove a imagem, e o monograma continua. Não há proxy de imagem no backend, então não há superfície nova de SSRF.
- **Login.** O conteúdo do painel passa a caber no cartão. `justify-content: safe center` garante que, se um dia não couber, corte embaixo e não o botão do topo.

## STRIDE

| Ameaça | Análise |
|---|---|
| Tampering / XSS | O monograma usa a primeira letra do ticker passada por `esc()`, como o resto da tabela. |
| Denial of service | As animações rodam só no cliente, têm duração fixa e não fazem chamadas extras. |
| Spoofing, repudiation, information disclosure, elevation of privilege | Não se aplicam: não há mudança de dados, rota ou permissão. |
