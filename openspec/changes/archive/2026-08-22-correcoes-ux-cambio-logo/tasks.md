## 1. Bugfix: notificações invisíveis

- [x] 1.1 Renomear `.toast` → `.rendo-toast` em `dashboard.css` (6 regras) e no `el.className` de `toast()` em `dashboard.js`
- [x] 1.2 Reproduzir e confirmar a causa raiz (colisão com `.toast:not(.show){display:none}` do Bootstrap) antes de aplicar a correção
- [x] 1.3 Verificar que `.nav` (mesmo padrão de nome coincidente com um componente Bootstrap) não tem colisão real de efeito — não precisa de correção

## 2. Câmbio via dolarapi.com

- [x] 2.1 Criar `DolarApiCambioClient` (retry + circuit breaker "cambio" reaproveitado, sem chave de API)
- [x] 2.2 Remover `TwelveDataCambioClient` e atualizar `AcaoService`/`OperacaoService` para o novo client
- [x] 2.3 Atualizar `AcaoServiceTest`/`OperacaoIntegrationTest` para o novo nome de classe

## 3. Logo das ações

- [x] 3.1 `DadosCotacaoResponse` ganha `logoUrl`
- [x] 3.2 `BrapiCotacaoProvider` captura `logourl` da resposta da brapi
- [x] 3.3 `TwelveDataCotacaoProvider` constrói a URL da FMP a partir do ticker
- [x] 3.4 `Acao`/`AcaoResponseDTO` ganham `logoUrl`, persistido em `criar()`/`atualizarCotacao()`
- [x] 3.5 `dashboard.js`: helper `acaoLogoHTML()` usado no catálogo (`renderAcoes`) e no detalhe (`detalheAcaoHTML`), com `onerror` removendo a imagem se falhar

## 4. Verificação

- [x] 4.1 `mvnw compile`/`mvnw test` passam
- [x] 4.2 Verificação manual no navegador: CNPJ inválido mostra erro; compra com sucesso mostra toast e fecha o modal; cadastro de ação mostra sucesso; compra acima do saldo mostra "saldo insuficiente"; logo aparece pra PETR4 (Brasil) e AAPL (EUA); conversão USD/BRL bate com a cotação da dolarapi.com
- [x] 4.3 Console do navegador e log do servidor sem erros novos
