## Context

Ver proposal.md - Why para o diagnóstico do bug de toast. `AcaoService`/`OperacaoService` já dependiam de um client de câmbio único (`TwelveDataCambioClient`, sem interface Strategy — só existe uma fonte). `BrapiCotacaoProvider`/`TwelveDataCotacaoProvider` já retornam `DadosCotacaoResponse` para `AcaoService.criar()`/`atualizarCotacao()` persistir em `Acao`.

## Goals / Non-Goals

**Goals:**
- Restaurar a visibilidade de toda notificação de sucesso/erro já implementada (sem reescrever a lógica de negócio, que já estava correta).
- Trocar a fonte de câmbio para uma API pública sem chave, sem mudar o contrato consumido por `AcaoService`/`OperacaoService`.
- Mostrar logo de empresa nas ações, sem nova chamada de rede quando a fonte já fornece o dado de graça (Brasil).

**Non-Goals:**
- Auditoria completa de outras colisões de nome com Bootstrap além de `.toast` — `.nav` foi verificado e não tem colisão real de efeito (mesma especificidade, sem `:not()`, propriedades conflitantes são todas sobrescritas ou inofensivas); não há motivo pra tocar nele.
- Cache/CDN próprio para os logos — usa as URLs públicas diretamente, sem proxy nem download local.
- Logo em Posições/Operações — só catálogo de ações e detalhe, conforme pedido.

## Decisions

- **Renomear `.toast` → `.rendo-toast`** em vez de sobrescrever a especificidade do Bootstrap (ex: com `!important` ou `.toast:not(.foo)`): mesma solução já usada para o mesmo problema em `.modal` → `.rendo-modal` neste projeto — consistente, sem gambiarra de especificidade que quebraria de novo na próxima atualização do Bootstrap.
- **dolarapi.com usa a taxa de "venda"**: é o custo real de adquirir dólares (o que a operação de compra em USD efetivamente debita em BRL), não "compra" (o que uma casa de câmbio pagaria pelo dólar de volta).
- **`DolarApiCambioClient` substitui `TwelveDataCambioClient` (não coexistem)**: a Twelve Data continua sendo usada só para cotação de ações EUA (`TwelveDataCotacaoProvider`), não para câmbio. Mantida a decisão original de não introduzir uma interface Strategy pra câmbio — ainda existe só uma fonte.
- **Logo EUA via FMP em vez do endpoint `/logo` da Twelve Data**: testado manualmente, o endpoint da Twelve Data retornou 404 em tickers válidos (AAPL) mesmo com chave configurada; o padrão `financialmodelingprep.com/image-stock/{ticker}.png` respondeu 200 de forma consistente em múltiplos tickers testados, sem exigir chave.
- **Falha ao carregar o logo é tratada só no frontend** (`onerror="this.remove()"` no `<img>`), não no backend: o backend sempre persiste a URL construída/recebida; se a imagem não existir de fato (ticket sem logo na FMP, por exemplo), o `<img>` se remove sozinho ao falhar o carregamento, sem quebrar o layout. Mais simples que validar a URL no backend a cada cadastro.

## Risks / Trade-offs

- [URL de logo persistida pode ficar desatualizada se o provedor mudar o padrão de URL no futuro] → aceito; mesmo nível de acoplamento a fonte externa que já existe pra cotação/CNPJ/CEP.
- [dolarapi.com é uma API pública nova no projeto, sem SLA formal] → mitigado pelo retry + circuit breaker já padrão nas integrações externas deste projeto (reaproveitados, não é uma exceção).

## Migration Plan

Coluna nova (`logo_url`) em `Acao` — recriada automaticamente em dev (`ddl-auto=create-drop`); em um ambiente com dado persistente real, seria uma migração aditiva simples (coluna nullable, sem backfill obrigatório). Rollback é reverter o commit — nenhuma migração de dado real envolvida neste projeto acadêmico (H2 em memória).
