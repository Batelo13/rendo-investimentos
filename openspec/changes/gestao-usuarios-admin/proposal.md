## Por quê

A aula 14 pede que o administrador gerencie usuários: criar contas, mudar o papel e redefinir a senha. Hoje o admin só bloqueia e desbloqueia, e o papel ADMIN só vem de `ADMIN_EMAILS`.

## O que muda

- **Criar conta pelo admin:** `POST /usuarios/admin` (ADMIN) com nome, email, CPF, senha e papel. A conta já nasce com e-mail verificado e com carteira. A senha segue a mesma regra do cadastro (8+, maiúscula e minúscula).
- **Trocar papel:** `PATCH /usuarios/{id}/papel` (ADMIN) com `{ "role": "USER" | "ADMIN" }`.
- **Redefinir senha:** `PUT /usuarios/{id}/senha` (ADMIN) com `{ "senha": "..." }`. Responde 204 e nunca devolve senha nem hash.
- **Proteções (422):**
  - o admin não muda o próprio papel nem bloqueia a si mesmo;
  - não dá para rebaixar o último ADMIN;
  - o papel de um email em `ADMIN_EMAILS` não muda pela API, porque voltaria a ser ADMIN na próxima inicialização.
- **Tela:** o painel de administração ganha o formulário "Criar conta" e, em cada linha, as ações "Tornar admin"/"Tornar usuário" e "Redefinir senha".
- **Spec modificada:** "Papel ADMIN concedido por configuracao" passa a aceitar também a promoção feita por um ADMIN.

## Impacto

- Endpoints novos, todos só para ADMIN: `POST /usuarios/admin`, `PATCH /usuarios/{id}/papel`, `PUT /usuarios/{id}/senha`.
- `PATCH /usuarios/{id}/bloquear` passa a recusar o próprio admin (422).
- Banco: nenhuma coluna nova.
