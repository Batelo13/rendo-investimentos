## Purpose

Exige que o usuário revise e confirme explicitamente uma compra ou venda de ação antes que a operação seja enviada e afete o saldo da carteira.

## Requirements

### Requirement: Confirmação antes de compra ou venda
O sistema SHALL exibir uma caixa de confirmação, apresentando o resumo da operação (valor total, resultado estimado e, quando aplicável, o valor convertido em reais), antes de enviar uma compra ou venda de ação para processamento.

#### Scenario: Usuário confirma a operação
- **WHEN** o usuário preenche o formulário de compra/venda com dados válidos e clica em "Confirmar compra"/"Confirmar venda"
- **THEN** o sistema exibe uma caixa de confirmação com o mesmo resumo já mostrado no formulário
- **AND** ao confirmar na caixa, a operação é enviada normalmente, o saldo é atualizado e uma mensagem de sucesso é exibida

#### Scenario: Usuário cancela na caixa de confirmação
- **WHEN** o usuário clica em "Confirmar compra"/"Confirmar venda" e depois cancela na caixa de confirmação
- **THEN** nenhuma operação é enviada, o saldo permanece inalterado, nenhuma mensagem de sucesso/erro é exibida, e o formulário continua aberto com os dados preenchidos

#### Scenario: Cadastro de ação ou corretora não exige confirmação
- **WHEN** o usuário cadastra uma nova ação ou corretora
- **THEN** o sistema não exibe caixa de confirmação, pois essas ações não afetam o saldo da carteira
