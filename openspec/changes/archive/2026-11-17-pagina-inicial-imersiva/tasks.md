## 1. Página

- [x] 1.1 Cabeçalho transparente que ganha fundo translúcido depois do scroll, com links reais.
- [x] 1.2 Jornada sticky com 4 momentos de texto e o indicador "Role para explorar".
- [x] 1.3 Cena SVG de fallback guiada pelo progresso (barras, personagem, linha, cidade ao amanhecer).
- [x] 1.4 Vídeo opcional sincronizado com o scroll, só depois dos metadados.
- [x] 1.5 Seções: o que é, como funciona, recursos, acompanhamento, segurança, chamada final, rodapé.
- [x] 1.6 `GET /videos/**` público. Teste: MockMvc sem login não recebe 401.

## 2. Validação

- [x] 2.1 Console sem erros, scroll para baixo e para cima. Teste: Playwright.
- [x] 2.2 Desktop, tablet e celular. Teste: screenshots em 3 tamanhos.
- [x] 2.3 `prefers-reduced-motion`: conteúdo estático e completo. Teste: Playwright.
- [x] 2.4 Sem vídeo e com vídeo lento. Teste: Playwright com vídeo de teste e rota atrasada.
- [x] 2.5 Suíte `mvnw test` e gate de segurança verdes.
