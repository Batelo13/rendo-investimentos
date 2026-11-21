## Por quê

A revisão da landing deixou pendências de acessibilidade: o texto dos passos inativos do tour ficava abaixo do contraste AA, e o leitor de tela lia o "antes" e o "depois" dos cards do problema sem distinguir um do outro. Além disso, o aviso do simulador não era anunciado, e o botão de câmbio usava `aria-pressed` com um rótulo que muda, sem anunciar o valor final.

## O que muda

- **Tour:** o texto dos passos inativos fica com opacidade 0,75, o que dá ≥ 4,5:1 sobre o fundo.
- **Cards do problema:** ganham os rótulos "Sem a Rendo:" e "Com a Rendo:", só para leitor de tela (classe `.lp-leitor`).
- **Aviso do simulador:** `#simAviso` passa a ser `role="status"` e sempre presente; o JS só troca o texto.
- **Botão de câmbio:** perde o `aria-pressed`. A região `#difCambioStatus` (`role="status"`) anuncia só o valor convertido final.

## Impacto

`index.html`, `landing.css`, `landing-simulador.js`, `landing-diferenciais.js`, `landing_regression.py`.
