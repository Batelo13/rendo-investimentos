## Why

O cadastro de ação (`POST /acoes`) aceita qualquer combinação de ticker + mercado sem validar se elas são de fato compatíveis. A brapi (provider de cotação para BRASIL) na prática também retorna dados para tickers americanos (ex.: INTC), então cadastrar "INTC" com mercado BRASIL é aceito silenciosamente, com a cotação vindo em dólar mas exibida sob a tag "BR Brasil" e sem a conversão para reais — um cadastro incorreto que só era perceptível visualmente. Isso motivou o botão de exclusão (`exclusao-acao-catalogo`); esta mudança ataca a causa raiz, rejeitando o cadastro incorreto antes de ele existir.

## What Changes

- No cadastro de ação (`POST /acoes`), após buscar a cotação no provider, valida se a moeda retornada é compatível com o mercado informado: BRASIL espera moeda `BRL`; EUA espera moeda `USD`.
- Se a moeda retornada não bater com o mercado selecionado, o cadastro é rejeitado (422) com uma mensagem explicando o motivo (ex.: "o ticker é negociado em USD, não é compatível com o mercado BRASIL").
- Não afeta `atualizarCotacao` (o mercado já foi validado na criação e não é alterável depois) nem ações já cadastradas antes desta mudança.

## Capabilities

### New Capabilities
- `validacao-mercado-cadastro-acao`: valida a compatibilidade entre o mercado declarado e a moeda real da cotação retornada pelo provider, ao cadastrar uma ação.

### Modified Capabilities
(nenhuma)

## Impact

- `AcaoService.criar()`: nova validação entre `buscarCotacao(...)` e a persistência da `Acao`.
- Nenhuma mudança de schema, endpoint novo ou contrato de resposta — o cadastro continua `POST /acoes`, só passa a rejeitar mais casos.
