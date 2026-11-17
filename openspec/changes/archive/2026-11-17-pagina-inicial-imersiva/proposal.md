## Why

A página inicial atual é um cartão centralizado com um botão. O usuário pediu uma página inicial imersiva e cinematográfica, no nível do vídeo de referência, que conte a jornada de evolução financeira e apresente o que a Rendo faz de verdade.

## What Changes

- **Jornada controlada pelo scroll:**
  - seção de 450vh (320vh no celular) com uma cena sticky de 100vh;
  - progresso do scroll × duração = `currentTime` do vídeo, com interpolação em `requestAnimationFrame`.
- **Fallback sem vídeo:** enquanto o vídeo final (`/videos/rendo-jornada.mp4`) não existir, ou se ele falhar, a mesma narrativa é desenhada numa cena SVG própria guiada pelo mesmo progresso.
- **Narrativa:** quatro momentos de texto (começo, planejamento, evolução, futuro), com transição de opacidade, deslocamento e desfoque.
- **Cabeçalho:** transparente no início, translúcido depois do scroll, com links para as seções reais, "Entrar" (`/login`) e "Criar conta" (`/login?criarConta=1`).
- **Seções seguintes, só com funcionalidades existentes:**
  - o que é a Rendo;
  - como funciona;
  - recursos;
  - acompanhamento da carteira;
  - segurança e privacidade;
  - chamada final;
  - rodapé.
- **Rota pública nova:** `GET /videos/**`, só leitura de estáticos.

## Impact

- **Arquivos:**
  - `templates/index.html` (reescrito);
  - `static/css/landing.css` e `static/js/landing.js` (novos);
  - `static/images/rendo-jornada-poster.webp` (novo);
  - `base.css` (perde o CSS da landing antiga);
  - `SecurityConfig` (uma linha).
- **Sem mudança em:** regras de negócio, banco, autenticação, endpoints de API ou dependências.
