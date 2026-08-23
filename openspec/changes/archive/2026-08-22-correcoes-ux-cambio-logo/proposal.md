## Why

Depois de testar o fluxo real de compra/cadastro no navegador, o usuário reportou 4 sintomas: CNPJ inválido no cadastro de corretora não avisa nada; confirmar uma compra parece não fazer nada; cadastro de ação não confirma sucesso/erro; comprar além do saldo disponível não avisa "saldo insuficiente". Investigando, os quatro eram o mesmo bug único: a classe `.toast` (usada pelas notificações da aplicação) colide com o componente `.toast` do Bootstrap, cuja regra `.toast:not(.show){display:none}` tem especificidade maior e vence — toda notificação era criada no DOM mas ficava com `display:none`, invisível. O mesmo padrão de colisão de nome com componente do Bootstrap já havia acontecido antes neste projeto (`.modal`, variáveis de `.table`).

Além disso, o usuário pediu duas melhorias: usar a dolarapi.com (fonte pública, sem chave) para a cotação USD/BRL em vez da Twelve Data; e mostrar o logo de cada ação cadastrada no catálogo.

## What Changes

- **BUGFIX**: notificações (toasts) de sucesso/erro renomeadas de `.toast` para `.rendo-toast` (CSS e JS) para não colidir com o componente `.toast` do Bootstrap. Isso restaura a visibilidade de: erro de CNPJ inválido/não-financeiro no cadastro de corretora, confirmação de compra/venda registrada, sucesso/erro no cadastro de ação, e erro de saldo insuficiente na compra — a lógica de cada um já existia e estava correta, só a notificação não aparecia.
- Fonte da cotação USD/BRL trocada de Twelve Data para **dolarapi.com** (`https://br.dolarapi.com/v1/cotacoes/usd`, pública, sem chave de API) — usa a taxa de "venda" (custo real de adquirir dólares). `TwelveDataCambioClient` removido, substituído por `DolarApiCambioClient`. Nenhuma mudança de contrato visível (mesmo método, mesmo uso em `AcaoService`/`OperacaoService`).
- Catálogo de ações e tela de detalhe passam a mostrar o **logo da empresa**: para ações do Brasil, o campo `logourl` já retornado pela brapi; para ações dos EUA, o padrão público `https://financialmodelingprep.com/image-stock/{ticker}.png` (o endpoint `/logo` da própria Twelve Data se mostrou instável). Logo é persistido em `Acao.logoUrl` e reatualizado a cada "atualizar cotação".

## Capabilities

### New Capabilities
- `logo-catalogo-acoes`: exibe o logo da empresa no catálogo de ações e na tela de detalhe, para ações cadastradas nos mercados Brasil e EUA.

### Modified Capabilities
(nenhuma — o bugfix de toast restaura comportamento já coberto implicitamente pelo design de `confirmacao-compra-venda`, e a troca de fonte de câmbio é um detalhe de implementação que não muda o contrato descrito em `conversao-cambio-eua`)

## Impact

- `dashboard.css`/`dashboard.js`: renomeia `.toast` → `.rendo-toast`.
- `integration/DolarApiCambioClient.java` (novo, substitui `TwelveDataCambioClient.java`, removido).
- `integration/BrapiCotacaoProvider.java`, `integration/TwelveDataCotacaoProvider.java`: capturam/constroem `logoUrl`.
- `integration/DadosCotacaoResponse.java`: novo campo `logoUrl`.
- `model/Acao.java`, `dto/AcaoResponseDTO.java`: novo campo `logoUrl`, persistido e exposto.
- `service/AcaoService.java`, `service/OperacaoService.java`: referência ao novo `DolarApiCambioClient`.
- Testes (`AcaoServiceTest`, `OperacaoIntegrationTest`): referência ao novo nome de classe.
