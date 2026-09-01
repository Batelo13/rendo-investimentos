## Purpose

Garante que uma ação só seja cadastrada com o mercado que de fato corresponde à moeda em que ela é negociada, evitando cadastros como um ticker americano registrado como mercado BRASIL (ou vice-versa).

## Requirements

### Requirement: Rejeição de cadastro com mercado incompatível com a moeda da cotação
O sistema SHALL rejeitar (HTTP 422) o cadastro de uma ação via `POST /acoes` quando a moeda retornada pelo provider de cotação não corresponder ao mercado informado: mercado `BRASIL` exige moeda `BRL`; mercado `EUA` exige moeda `USD`.

#### Scenario: Ticker americano cadastrado como BRASIL
- **WHEN** um usuário cadastra o ticker `INTC` com mercado `BRASIL`, e o provider de cotação retorna a moeda `USD` para esse ticker
- **THEN** o sistema rejeita o cadastro com um erro de regra de negócio explicando que o ticker não é compatível com o mercado BRASIL, e nenhuma ação é criada

#### Scenario: Ticker brasileiro cadastrado como EUA
- **WHEN** um usuário cadastra o ticker `PETR4` com mercado `EUA`, e o provider de cotação retorna a moeda `BRL` para esse ticker
- **THEN** o sistema rejeita o cadastro com um erro de regra de negócio explicando que o ticker não é compatível com o mercado EUA, e nenhuma ação é criada

#### Scenario: Cadastro com mercado compatível continua funcionando
- **WHEN** um usuário cadastra um ticker cujo mercado informado corresponde à moeda retornada pelo provider (ex.: `PETR4` com mercado `BRASIL` e moeda `BRL`, ou `AAPL` com mercado `EUA` e moeda `USD`)
- **THEN** o sistema cadastra a ação normalmente, como antes desta mudança
