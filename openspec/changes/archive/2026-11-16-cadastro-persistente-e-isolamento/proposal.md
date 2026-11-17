## Why

Pedidos do usuario e do professor (Modulo 11) apos rodar o sistema:

1. **Cadastros se perdiam:** o profile dev usa H2 em memoria com `create-drop`; todo cadastro some
   ao parar o servidor.
2. **Dado sensivel armazenado:** o CPF fica em texto puro no banco e e devolvido na API
   (`GET /usuarios`, `GET /usuarios/{id}`), embora nenhuma tela o use -- serve so para impedir
   CPF duplicado. A mensagem de duplicidade tambem devolve o CPF informado.
3. **Administrador:** nao ha forma de conceder papel ADMIN a um usuario real sem editar o banco.
4. **Isolamento entre contas:** o professor pediu garantia de que copiar o link do dashboard (ou
   qualquer rota) de outra pessoa nao da acesso aos dados dela.

## What Changes

- Profile dev persiste em H2 **em arquivo** (`./data/`, fora do Git) com `ddl-auto=update`; os
  testes seguem com banco em memoria proprio.
- **BREAKING:** CPF armazenado apenas como HMAC-SHA256 com chave secreta (`CPF_HASH_KEY`); o campo
  `cpf` sai da resposta de `GET /usuarios` e `GET /usuarios/{id}`; a mensagem de CPF duplicado deixa
  de repetir o numero. CPFs ja gravados em texto puro sao convertidos na inicializacao.
- Emails listados em `ADMIN_EMAILS` recebem papel ADMIN no cadastro e na inicializacao.
- Testes de isolamento: com a sessao de um usuario, toda rota que recebe id de outro usuario
  responde 403, e as rotas `/me` e `/dashboard` mostram apenas os dados da propria sessao.

## Capabilities

### New Capabilities
- `protecao-dados-cadastro`: persistencia dos cadastros, CPF so como hash, concessao de ADMIN por
  configuracao e isolamento de dados entre contas.

### Modified Capabilities
(nenhuma)

## Impact

- Endpoints: `GET /usuarios`, `GET /usuarios/{id}` (sem `cpf`); `POST /usuarios` (409 sem eco do CPF).
- Banco: coluna `usuarios.cpf` passa a guardar hash hex de 64 caracteres (mesmo tipo varchar).
- Variaveis novas: `CPF_HASH_KEY` (obrigatoria fora do dev), `ADMIN_EMAILS`.
