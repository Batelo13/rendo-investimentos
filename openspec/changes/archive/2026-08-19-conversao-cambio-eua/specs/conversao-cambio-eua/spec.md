## Purpose

Garante que operações e cotações de ações do mercado EUA sejam convertidas de USD para BRL de forma correta e consistente com o saldo (que é todo em reais), usando sempre a taxa de câmbio vigente no momento de cada operação — nunca uma taxa "de agora" recalculada — e exibindo o valor convertido ao lado do valor original sem escondê-lo.

## ADDED Requirements

### Requirement: Saldo é descontado/creditado pelo valor convertido, não pelo valor bruto em dólar
O sistema SHALL calcular o impacto no saldo de uma operação em ação EUA multiplicando `precoUnitario × quantidade` pela taxa de câmbio USD→BRL vigente no momento da operação. Para ações BRASIL, a taxa é sempre `1` e o comportamento permanece idêntico ao anterior.

#### Scenario: Compra de ação EUA desconta o valor convertido
- **WHEN** um usuário compra uma ação EUA a um preço em dólar
- **THEN** o saldo é descontado no valor equivalente em reais (preço × quantidade × taxa de câmbio), não no número bruto em dólar

#### Scenario: Saldo insuficiente mostra o valor já convertido
- **WHEN** uma compra de ação EUA excede o saldo disponível após a conversão
- **THEN** o sistema rejeita a operação com uma mensagem que mostra o custo já convertido para reais

### Requirement: Taxa de câmbio é gravada de forma imutável em cada operação
O sistema SHALL buscar a taxa de câmbio USD→BRL no momento do registro de uma operação em ação EUA e gravá-la na própria `Operacao`, nunca recalculando com uma taxa atual em consultas futuras.

#### Scenario: Registro de operação EUA busca e grava a taxa vigente
- **WHEN** uma operação de compra ou venda de ação EUA é registrada
- **THEN** o sistema busca a taxa de câmbio USD→BRL vigente e a grava na operação

#### Scenario: Falha ao buscar a taxa impede o registro
- **WHEN** a busca da taxa de câmbio falha durante o registro de uma operação EUA
- **THEN** o sistema rejeita a operação com erro de serviço externo indisponível, sem registrar a operação

#### Scenario: Histórico e resultado usam a taxa gravada, não a taxa atual
- **WHEN** o histórico de operações ou o cálculo de resultado ("realizado"/"não realizado") de uma posição EUA é exibido
- **THEN** o sistema usa a taxa de câmbio gravada em cada operação, não a taxa de câmbio atual

### Requirement: Cotação de ações EUA é exposta também em reais, com degradação graciosa
O sistema SHALL expor a cotação atual de uma ação EUA convertida para reais, retornando `null` quando a ação for do mercado BRASIL ou quando a busca da taxa de câmbio falhar, sem impedir a listagem.

#### Scenario: Ação EUA com cotação disponível
- **WHEN** uma ação EUA tem `cotacaoAtual` e a busca da taxa de câmbio é bem-sucedida
- **THEN** a resposta inclui `cotacaoAtualBRL` com o valor convertido

#### Scenario: Ação BRASIL nunca tem cotação convertida
- **WHEN** uma ação é do mercado BRASIL
- **THEN** `cotacaoAtualBRL` é sempre `null`

#### Scenario: Falha na busca da taxa não derruba a listagem
- **WHEN** a busca da taxa de câmbio falha durante uma consulta de leitura (listagem ou detalhe de ação)
- **THEN** a listagem prossegue normalmente com `cotacaoAtualBRL = null` para as ações EUA afetadas

### Requirement: Interface mostra o valor convertido ao lado do valor original
O sistema SHALL exibir, para toda ação EUA com cotação convertida disponível, o valor em reais como uma linha secundária ao lado do valor em dólar — no catálogo de ações, no painel de posições e no resumo do modal de compra/venda — sem nunca substituir o valor original.

#### Scenario: Catálogo de ações mostra a conversão
- **WHEN** o catálogo de ações lista uma ação EUA com cotação convertida disponível
- **THEN** a linha da ação mostra o valor em dólar com uma linha "≈ R$X,XX" abaixo

#### Scenario: Resumo de compra/venda mostra o valor convertido antes de confirmar
- **WHEN** um usuário preenche o formulário de compra/venda de uma ação EUA
- **THEN** o resumo mostra o valor total e o resultado estimado em dólar, cada um com a linha convertida em reais, atualizando ao vivo

#### Scenario: "Não realizado" soma posições convertendo cada uma antes de somar
- **WHEN** a Visão Geral calcula o card "Não realizado" com posições em mais de uma moeda
- **THEN** cada posição EUA é convertida para reais pela sua própria taxa antes da soma, evitando misturar USD e BRL no total
