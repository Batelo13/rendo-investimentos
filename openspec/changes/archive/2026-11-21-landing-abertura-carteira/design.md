## Decisões

- **Sem dependência nova** (abordagem A do design): SVG, JS puro, Web Animations API e IntersectionObserver. GSAP e Three.js foram descartados (peso e GPU sem ganho visível).
- **Cartão em HTML, não em SVG:** perspectiva 3D em CSS para a inclinação. O SVG fica dentro dele, para o gráfico e o anel.
- **O motor de progresso continua:** o `p` interpolado em rAF, os momentos de texto e o foco por teclado não mudam; só o desenho da cena é trocado.
- **Vídeo removido:** a decisão do usuário é a cena ilustrada. Código morto e uma rota pública a menos.
- **Toque:** sem giroscópio (no iOS ele pede permissão). Inclinação e luz só com `(hover: hover) and (pointer: fine)`.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Sem autenticação nova. |
| Tampering | Nada é enviado ao servidor; a cena é só apresentação. |
| Repudiation | Não se aplica. |
| Information disclosure | Superfície pública menor (`/videos/**` sai). Os números são demonstrativos e rotulados; nenhum dado de usuário na landing. |
| Denial of service | Animações só de `transform`/`opacity`, ponteiro agrupado em rAF; sem processamento no servidor. |
| Elevation of privilege | A CSP `script-src 'self'` se mantém: todo JS em arquivo, nada inline. |
