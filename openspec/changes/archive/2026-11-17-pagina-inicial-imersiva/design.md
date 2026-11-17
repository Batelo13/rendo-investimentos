## Decisões

- **Sem biblioteca de animação.** O projeto não tem nenhuma. Scroll, `requestAnimationFrame` e transforms cobrem o pedido, e GSAP/ScrollTrigger seria uma dependência grande para um uso só.
- **Uma engine, duas mídias.** `landing.js` calcula o progresso `p` (0..1) e o interpola: `atual += (alvo - atual) * 0.14` por quadro, e o loop para quando converge.
  - Com vídeo: `currentTime = p × duration`, só depois de `loadedmetadata`, e sem novo seek enquanto `video.seeking`.
  - Sem vídeo: a cena SVG recebe `p` e posiciona barras, personagem, linha, céu e cidade.
- **Vídeo opt-in por atributo.** `data-video` e `data-video-mobile` na seção. Com eles vazios, nenhum pedido é feito: não há 404 no console nem download inútil.
  - O vídeo é pedido só depois do `load` da página, com `preload="metadata"`.
  - A versão de celular é usada abaixo de 768px.
  - Com economia de dados ligada, o vídeo não é carregado.
  - Em `error`, o celular tenta a versão desktop e depois cai na cena SVG.
- **Movimento reduzido.** Não há scrub. A cena fica parada no estado final e os quatro momentos viram texto em fluxo normal.
- **Teclado.** O foco num momento rola a página até a faixa desse momento, então os botões nunca ficam focados e invisíveis. Momentos invisíveis não recebem clique (`pointer-events: none`).
- **Honestidade.** Os textos falam de simulação com saldo fictício. Não há promessa de retorno, metas ou "segurança absoluta".

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Não há autenticação nova. Os botões levam às rotas existentes. |
| Tampering | `/videos/**` é só GET de arquivo estático. Uploads não existem. |
| Repudiation | Não se aplica. |
| Information disclosure | `/videos/**` publica só o que for colocado em `static/videos`, que é conteúdo público por natureza. Nenhum dado de usuário passa pela landing. |
| Denial of service | O vídeo é servido como estático (com suporte a Range do Spring). Não há processamento no servidor. |
| Elevation of privilege | A liberação vale só para `GET /videos/**`. O resto da cadeia de segurança não muda. |
