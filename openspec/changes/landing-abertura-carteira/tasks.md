## 1. Vídeo

- [x] 1.1 Remover vídeo, poster e `GET /videos/**`. Teste: `SecurityHeadersIntegrationTest.assetsDaPaginaInicialSaoPublicos` e `LiquidGlassMarkupTest.landingNaoTemVideo`.

## 2. Sistema de movimento

- [x] 2.1 `data-contador`, `data-luz` e `data-inclina` + fatos com contador. Teste: `LiquidGlassMarkupTest.landingUsaContadorNosFatos`; Playwright `contador_anima_ate_valor_final`, `contador_respeita_movimento_reduzido`, `toque_nao_inclina` e `sem_js_mostra_valores_finais`.

## 3. Abertura

- [ ] 3.1 Cartão da carteira demonstrativa dirigido pelo scroll. Teste: `LiquidGlassMarkupTest.aberturaTemCartaoDemonstrativo`; Playwright `cena_acompanha_scroll`, `scroll_brusco_mantem_cena_coerente`, `redimensionar_sem_overflow`, `ponteiro_inclina_cartao`, `teclado_foca_botoes_visiveis`, `movimento_reduzido_mostra_estado_final` e `screenshots`.

## 4. Cabeçalho e SEO

- [ ] 4.1 Link da seção ativa, luz no CTA e Open Graph. Teste: `LiquidGlassMarkupTest.landingTemOpenGraph`; Playwright `cabecalho_marca_secao_ativa`.

## 5. Validação

- [ ] 5.1 Suíte, roteiro Playwright, desempenho (sem tarefa > 50 ms recorrente, CLS < 0,1), gate e DAST aprovados.
