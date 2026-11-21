## Decisões

- **Chave IP+email, não só email.** Um bloqueio só por email deixaria qualquer pessoa trancar a conta de outra. Com IP+email, o dono continua entrando de outro IP. O limite por IP cobre o atacante que varia o email.
- **Em memória, sem dependência.** Um `ConcurrentHashMap` com janela fixa de 15 min resolve, e as entradas expiradas são limpas acima de 10 mil chaves. Bucket4j ou Redis só fariam sentido com mais de uma instância (marcado com `ponytail:` no código).
- **Bloqueio antes da autenticação.** Um filtro antes do filtro de login responde sem rodar o BCrypt, o que também poupa CPU sob ataque.
- **"Email não verificado" não conta como falha.** Esse caso é barrado antes de checar a senha, então não indica adivinhação.
- **IP = `remoteAddr`.** `X-Forwarded-For` não é usado, porque pode ser forjado sem um proxy confiável. Atrás de proxy reverso, o IP passa a ser o do proxy e o limite por IP vira global; isso precisa ser revisto no deploy.
- **Login social fora.** Quem autentica é o provedor, que tem a própria proteção.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Força bruta e credential stuffing ficam limitados a 5 senhas por par e 20 por IP a cada 15 min. |
| Tampering | Não se aplica. |
| Repudiation | Não se aplica (sem log novo de tentativas). |
| Information disclosure | A resposta de bloqueio é igual para email existente e inexistente: não revela contas. |
| Denial of service | O atacante só bloqueia o próprio IP, e o dono entra de outro. Risco aceito: usuários atrás do mesmo NAT podem se bloquear juntos depois de 20 falhas. A memória é limitada pela limpeza das expiradas. |
| Elevation of privilege | Sem mudança em papéis. |
