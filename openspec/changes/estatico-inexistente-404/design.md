## Decisões

- **Liberar `/error`, não criar controller de erro.** É a correção padrão do Spring Security para esse sintoma: a cadeia de filtros roda de novo no dispatch de erro, e a página de erro precisa ser acessível a quem já tinha acesso ao recurso original.
- **Sem mudança no entry point.** Pedido sem sessão a rota protegida é barrado com 401 antes do dispatch de erro, então liberar `/error` não troca 401 por 404 nessas rotas.

## STRIDE

| Ameaça | Análise |
|---|---|
| Spoofing | Não há autenticação nova. |
| Tampering | `/error` só renderiza a resposta de erro; não altera estado. |
| Repudiation | Não se aplica. |
| Information disclosure | A página de erro padrão não inclui stack trace nem mensagem (padrões `never` do Spring Boot). Rotas protegidas inexistentes continuam 401, sem enumeração. Teste cobre. |
| Denial of service | Sem processamento novo. |
| Elevation of privilege | Acesso direto a `/error` não dá acesso a nenhum recurso protegido. |
