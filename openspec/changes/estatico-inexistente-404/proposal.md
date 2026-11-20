## Why

Um arquivo estático público inexistente (ex.: `GET /css/nao-existe.css`) responde 401 em vez de 404. O Spring Boot despacha o erro internamente para `/error`, que não estava liberado e caía na regra "qualquer outra rota exige login". O visitante e o navegador recebem um status enganoso.

## What Changes

- `/error` passa a ser público, para que o 404 dos estáticos públicos chegue ao cliente.
- Rotas protegidas sem sessão continuam respondendo 401 (inclusive as inexistentes), sem revelar quais existem.

## Impact

- **Endpoints públicos:** só `/error` (página de erro padrão, sem stack trace nem mensagem).
- **Banco:** sem mudança.
- **Arquivos:** `SecurityConfig` (uma regra) e `SecurityHeadersIntegrationTest` (dois testes).
