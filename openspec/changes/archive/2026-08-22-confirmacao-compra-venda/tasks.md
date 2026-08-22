## 1. Vendorizar o SweetAlert2

- [x] 1.1 Copiar `sweetalert2.all.min.js` de `node_modules` para `static/js/vendor/`
- [x] 1.2 Adicionar o script vendorizado em `dashboard.html`, carregado antes de `dashboard.js`
- [x] 1.3 Ignorar `node_modules/` no `.gitignore` (mantendo `package.json`/`package-lock.json` versionados)

## 2. Estilizar a caixa com os tokens Rendo

- [x] 2.1 Adicionar bloco CSS para `.swal2-popup`/`.swal2-title`/`.swal2-html-container`/`.swal2-confirm`/`.swal2-cancel`/`.swal-resumo` em `dashboard.css`, usando os tokens `--rendo-color-*` existentes

## 3. Adicionar a confirmação em `submitOperacao()`

- [x] 3.1 Inserir `await Swal.fire(...)` (reaproveitando o HTML de `#opResumo`) entre as validações de campo e a chamada à API
- [x] 3.2 Retornar sem enviar nada quando `isConfirmed` for falso
- [x] 3.3 Checar sintaxe (`node --check dashboard.js`)

## 4. Verificação manual no navegador

- [x] 4.1 Comprar uma ação: confirmar que a caixa mostra o resumo correto (valor total, resultado estimado, conversão em R$ para ação EUA)
- [x] 4.2 Cancelar na caixa: confirmar que nada é enviado, saldo inalterado, modal continua aberto com os dados preenchidos
- [x] 4.3 Confirmar na caixa: operação registrada normalmente (toast de sucesso, modal fecha, saldo/posições atualizam)
- [x] 4.4 Repetir para venda
- [x] 4.5 Alternar tema claro/escuro com a caixa aberta: cores acompanham o tema, sem contraste quebrado
- [x] 4.6 Conferir console do navegador sem erros novos
