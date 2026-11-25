## Decisões

1. **Conta criada pelo admin já vem verificada.** O admin responde por ela, e é a forma de dar acesso a alguém sem depender do SMTP. Ela segue as mesmas regras do cadastro: senha forte, CPF válido e protegido por HMAC, email e CPF únicos (409), e uma carteira com o saldo virtual inicial.
2. **A criação reaproveita o cadastro** (mesma validação do DTO e mesma montagem da entidade). O que muda é só o papel, o `emailVerified` e o envio do código.
3. **`ADMIN_EMAILS` continua sendo a fonte de verdade para quem está nele.** Rebaixar esse email pela API seria desfeito na próxima inicialização, então a API recusa com 422 e explica o motivo.
4. **Sem autoalteração.** O admin não muda o próprio papel nem se bloqueia, para não perder o acesso por engano.
5. **Sempre sobra um ADMIN.** Rebaixar o último ADMIN é recusado.
6. **Redefinir senha** passa pelo BCrypt e responde 204, sem corpo. Sessões e tokens já emitidos continuam valendo até expirar; esse limite já estava registrado na change anterior.
7. **Tela:** formulário no próprio painel, sem modal, para ser acessível. As ações de papel e de senha confirmam antes, com SweetAlert, como o bloqueio já faz.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Só um ADMIN autenticado (sessão ou JWT) chega às rotas (`@PreAuthorize`). |
| Tampering | Papel, `ativo` e `emailVerified` só mudam pelas rotas de ADMIN. Para quem não é admin, o cadastro público continua ignorando esses campos. |
| Repudiation | Sem trilha de auditoria (limite já registrado). As ações não apagam nada. |
| Information disclosure | As respostas usam o `UsuarioResponseDTO`, sem senha, hash nem CPF. Redefinir senha responde 204. |
| Denial of service | Sem autobloqueio e sem rebaixar o último admin: o sistema nunca fica sem administrador. |
| Elevation of privilege | Promover alguém exige ser ADMIN. Um USER recebe 403 em todas as rotas novas. |
