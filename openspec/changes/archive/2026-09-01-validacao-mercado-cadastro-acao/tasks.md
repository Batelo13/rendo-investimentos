## 1. Backend

- [x] 1.1 Em `AcaoService`, adicionar um mapa/constante de moeda esperada por `Mercado` (`BRASIL` → `BRL`, `EUA` → `USD`).
- [x] 1.2 Em `criar()`, logo após `buscarCotacao(...)`, validar se `dadosCotacao.moeda()` bate com a moeda esperada para `dto.mercado()`; se não bater, lançar `RegraDeNegocioException` com mensagem citando o ticker, a moeda retornada e o mercado informado, sem persistir nada.

## 2. Testes automatizados

- [x] 2.1 Em `AcaoServiceTest`, adicionar um teste que cadastra um ticker com mercado BRASIL mas provider retornando moeda USD, e verifica que `RegraDeNegocioException` é lançada e `repository.save` nunca é chamado.
- [x] 2.2 Adicionar o cenário inverso (mercado EUA, moeda BRL retornada).
- [x] 2.3 Confirmar que o teste existente de cadastro com mercado/moeda compatíveis continua passando sem alteração.

## 3. Verificação manual

- [x] 3.1 Rodar a aplicação localmente e tentar cadastrar INTC como mercado Brasil — confirmar que a interface exibe a mensagem de erro e a ação não aparece no catálogo.
- [x] 3.2 Cadastrar INTC como mercado EUA (correto) e confirmar que funciona normalmente.
- [x] 3.3 Rodar a suíte de testes (`./mvnw test`, com `MAIL_ENABLED=false`) e confirmar que nada quebrou.
