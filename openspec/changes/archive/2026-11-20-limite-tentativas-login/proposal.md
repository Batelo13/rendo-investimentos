## Why

O login por formulário aceitava tentativas ilimitadas. Um atacante podia testar senhas sem parar (força bruta) ou testar uma senha comum em muitos emails (credential stuffing).

## What Changes

- **Limite por par:** 5 falhas do mesmo IP para o mesmo email bloqueiam esse par por 15 minutos, mesmo com a senha certa.
- **Limite por IP:** 20 falhas do mesmo IP, em quaisquer emails, bloqueiam o IP por 15 minutos.
- **Login certo** zera as falhas daquele par.
- **Pedido bloqueado** volta para `/login?erro=muitas-tentativas`, com a mensagem "Muitas tentativas de login. Aguarde 15 minutos e tente de novo." A senha nem chega a ser verificada.

## Impact

- **Endpoints:** `POST /login` ganha o redirect para `/login?erro=muitas-tentativas`. Nenhum endpoint novo.
- **Banco:** sem mudança. O contador fica em memória.
- **Dependências:** nenhuma.
