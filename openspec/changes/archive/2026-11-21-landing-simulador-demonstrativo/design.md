## Decisões

- **Modelo ilustrativo e determinístico.** Um índice de mercado por cenário (13 pontos) e um beta por ação: `fator = 1 + beta × (índice − 1) + 0,006 × sen(t × (1,1 + 0,3k))`. Carteira toda em caixa não varia, e a mesma entrada sempre dá o mesmo gráfico.
- **Cenários em vez de retorno único.** Mostrar que o resultado depende do mercado evita soar como promessa de rentabilidade.
- **Nada sai do navegador.** Não há chamada à API de cotações (a landing é pública), nada fica salvo e nada é levado ao cadastro.
- **Controles nativos.** `input type=range` e `input type=radio`: teclado, leitor de tela e toque funcionam sem código extra.
- **Eixo Y fixo** (R$ 85.000 a R$ 120.000), para comparar os cenários a olho.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Sem autenticação envolvida. |
| Tampering | O estado vive só no navegador do visitante; não há entrada no servidor. |
| Repudiation | Não se aplica. |
| Information disclosure | Nenhum dado pessoal; os números são demonstrativos e rotulados. |
| Denial of service | Sem processamento no servidor; o redesenho é limitado a um por quadro. |
| Elevation of privilege | Todo JS é arquivo próprio (CSP `script-src 'self'` mantida). |
