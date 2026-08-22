## Why

O modal de compra/venda envia a operação para a API assim que o botão "Confirmar compra"/"Confirmar venda" é clicado — não existe nenhum passo de confirmação entre o clique e a operação afetar o saldo da carteira. Como compra/venda é a única ação do dashboard que mexe em dinheiro, um clique acidental ou um valor digitado errado vira operação imediatamente, sem chance de revisão. `sweetalert2` já estava instalado (`package.json`) mas não era usado em nenhum lugar do código.

## What Changes

- Antes de enviar `POST /operacoes`, `submitOperacao()` abre uma caixa de confirmação (SweetAlert2) mostrando o mesmo resumo ao vivo que o modal já monta (valor total, resultado estimado, conversão em R$ para ações EUA).
- Cancelar na caixa não envia nada: o modal de compra/venda continua aberto com os campos preenchidos, saldo inalterado, nenhum toast.
- Confirmar na caixa segue o fluxo já existente sem mudança: `POST /operacoes`, toast de sucesso/erro, fechamento do modal, recarga dos dados.
- `sweetalert2` vendorizado localmente em `static/js/vendor/sweetalert2.all.min.js` (cópia de `node_modules`, sem CDN, sem build step).
- Caixa estilizada com os tokens de cor já existentes (`--rendo-color-*`), acompanhando automaticamente o tema claro/escuro via `var()`.
- Cadastro de ação/corretora continua sem confirmação — não mexe em saldo.
- Toasts de sucesso/erro (`toast()`) não são substituídos — continuam sendo o único canal de feedback pós-operação.

## Capabilities

### New Capabilities
- `confirmacao-compra-venda`: exige uma etapa de confirmação explícita (aceitar/cancelar) antes de qualquer compra ou venda de ação afetar o saldo da carteira.

### Modified Capabilities
(nenhuma — não altera contrato de API nem regra de negócio existente, apenas insere confirmação no fluxo de frontend)

## Impact

- `dashboard.html`: novo `<script>` local para o SweetAlert2 vendorizado, carregado antes de `dashboard.js`.
- `static/js/vendor/sweetalert2.all.min.js` (novo arquivo, vendorizado).
- `dashboard.js`: `submitOperacao()` ganha um `await Swal.fire(...)` antes da chamada à API.
- `dashboard.css`: novo bloco estilizando `.swal2-*` com os tokens Rendo.
- Nenhuma mudança em backend, API ou modelo de dados.
