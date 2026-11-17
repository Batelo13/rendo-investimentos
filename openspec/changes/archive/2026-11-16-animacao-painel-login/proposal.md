## Why

O usuário pediu que a tela de login e cadastro tenha as mesmas animações de números e textos que o dashboard ganhou na change `animacoes-e-ajustes-visuais`.

## What Changes

O painel lateral visível ganha três efeitos, na primeira carga e a cada troca entre login e cadastro:

- os textos entram em cascata com fade e blur;
- os números dos widgets contam a partir de zero;
- as linhas dos minigráficos se desenham.

Com `prefers-reduced-motion` ligado, não há animação.

## Impact

Front-end apenas: `login.js`, `login.html` (`pathLength` nas polylines) e `login.css`. Não há chamadas de API: os widgets continuam estáticos.
